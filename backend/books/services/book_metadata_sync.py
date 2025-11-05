"""
Services for synchronising book publication records with the Kakao Books API.
"""

from __future__ import annotations

import logging
import os
from dataclasses import dataclass
from typing import Iterable, Optional
from urllib.parse import urlsplit

import requests
from books.models import Author, BookPublication, Genre, Publisher, Translator
from django.core.files.base import ContentFile
from django.db import transaction
from django.db.models import QuerySet
from django.utils.text import slugify

from .kakao_book_pipeline import (
    ExternalBook,
    ExternalBookAPIError,
    KakaoBookPipeline,
)

logger = logging.getLogger(__name__)


@dataclass
class SyncResult:
    """
    Result summary for batch synchronisation.
    """

    processed: int
    updated: int
    failures: int


class BookMetadataSynchronizer:
    """Enrich internal publication metadata using Kakao book data."""

    IMAGE_TIMEOUT = 10

    def __init__(
        self,
        pipeline: Optional[KakaoBookPipeline] = None,
        session: Optional[requests.Session] = None,
    ) -> None:
        self.pipeline = pipeline or KakaoBookPipeline()
        self.session = session or requests.Session()

    def sync_book(
        self, publication: BookPublication, *, overwrite: bool = False
    ) -> bool:
        """Enrich a single publication."""
        query = self._determine_query(publication)
        if not query:
            logger.debug(
                "Skipping publication %s due to missing query",
                publication.id,
            )
            return False

        try:
            candidates = self.pipeline.fetch(query, size=5)
        except ExternalBookAPIError:
            logger.warning(
                "Kakao API call failed while syncing publication %s",
                publication.id,
                exc_info=True,
            )
            raise

        match = self._select_match(publication, candidates)
        if not match:
            logger.debug(
                "No external match found for publication %s",
                publication.id,
            )
            return False

        return self.apply_metadata(publication, match, overwrite=overwrite)

    def sync_queryset(
        self,
        queryset: QuerySet[BookPublication],
        *,
        overwrite: bool = False,
        limit: Optional[int] = None,
    ) -> SyncResult:
        """
        Loop through a queryset of books and enrich them sequentially.
        """
        processed = 0
        updated = 0
        failures = 0

        for publication in queryset.iterator():
            if limit is not None and processed >= limit:
                break

            processed += 1

            try:
                changed = self.sync_book(publication, overwrite=overwrite)
            except ExternalBookAPIError:
                failures += 1
                continue

            if changed:
                updated += 1

        return SyncResult(
            processed=processed, updated=updated, failures=failures
        )

    def _determine_query(self, publication: BookPublication) -> Optional[str]:
        if publication.isbn_13:
            return publication.isbn_13
        if publication.isbn_10:
            return publication.isbn_10
        title = (publication.title or "").strip()
        if title:
            return title
        return None

    def _select_match(
        self,
        publication: BookPublication,
        candidates: Iterable[ExternalBook],
    ) -> Optional[ExternalBook]:
        candidates = list(candidates)
        if not candidates:
            return None

        isbn_priority = [
            publication.isbn_13 or "",
            publication.isbn_10 or "",
        ]
        for isbn in isbn_priority:
            if not isbn:
                continue
            for candidate in candidates:
                if candidate.isbn == isbn:
                    return candidate

        for candidate in candidates:
            if candidate.title.lower() == publication.title.lower():
                return candidate

        return candidates[0]

    @transaction.atomic
    def apply_metadata(
        self,
        publication: BookPublication,
        metadata: ExternalBook,
        *,
        overwrite: bool,
    ) -> bool:
        fields_updated = set()
        changed = False

        # Sync description (contents from Kakao API)
        if metadata.description and (overwrite or not publication.description):
            publication.description = metadata.description
            fields_updated.add("description")

        # Sync external URL
        if metadata.url and (overwrite or not publication.external_url):
            publication.external_url = metadata.url
            fields_updated.add("external_url")

        # Sync publication date
        if metadata.publication_date and (
            overwrite or not publication.publication_date
        ):
            # Parse ISO 8601 datetime to date
            pub_date = self._parse_publication_date(metadata.publication_date)
            if pub_date:
                publication.publication_date = pub_date
                fields_updated.add("publication_date")

        # Sync prices
        if metadata.price is not None and (
            overwrite or publication.original_price is None
        ):
            publication.original_price = metadata.price
            fields_updated.add("original_price")

        if metadata.sale_price is not None and (
            overwrite or publication.sale_price is None
        ):
            publication.sale_price = metadata.sale_price
            fields_updated.add("sale_price")

        # Sync sales status
        if metadata.status and (overwrite or not publication.sales_status):
            publication.sales_status = metadata.status
            fields_updated.add("sales_status")

        genres_changed = self._sync_genres(
            publication, metadata.categories, overwrite
        )
        authors_changed = self._sync_authors(
            publication, metadata.authors, overwrite
        )
        translators_changed = self._sync_translators(
            publication, metadata.translators, overwrite
        )
        cover_changed = self._sync_cover(
            publication, metadata.thumbnail_url, overwrite
        )

        publisher_changed = False
        if metadata.publisher and (overwrite or publication.publisher is None):
            publisher_obj, _ = Publisher.objects.get_or_create(
                name=metadata.publisher
            )
            if publication.publisher != publisher_obj:
                publication.publisher = publisher_obj
                fields_updated.add("publisher")
                publisher_changed = True

        isbn_fields = self._sync_isbn(publication, metadata.isbn, overwrite)
        if isbn_fields:
            fields_updated.update(isbn_fields)
            isbn_changed = True
        else:
            isbn_changed = False

        if fields_updated:
            publication.save(
                update_fields=list(fields_updated) + ["updated_at"]
            )
            changed = True

        if (
            authors_changed
            or translators_changed
            or genres_changed
            or cover_changed
            or publisher_changed
            or isbn_changed
        ):
            changed = True

        return changed

    def _sync_authors(
        self,
        publication: BookPublication,
        authors: Iterable[str],
        overwrite: bool,
    ) -> bool:
        authors = [author.strip() for author in authors if author.strip()]
        if not authors:
            return False

        existing_names = {author.name for author in publication.authors.all()}
        changed = False

        if overwrite:
            publication.authors.clear()
            existing_names.clear()

        for author_name in authors:
            if author_name in existing_names:
                continue
            author_obj, _ = Author.objects.get_or_create(name=author_name)
            publication.authors.add(author_obj)
            changed = True

        return changed

    def _sync_translators(
        self,
        publication: BookPublication,
        translators: Iterable[str],
        overwrite: bool,
    ) -> bool:
        translators = [
            translator.strip()
            for translator in translators
            if translator.strip()
        ]
        if not translators:
            return False

        existing_names = {
            translator.name for translator in publication.translators.all()
        }
        changed = False

        if overwrite:
            publication.translators.clear()
            existing_names.clear()

        for translator_name in translators:
            if translator_name in existing_names:
                continue
            translator_obj, _ = Translator.objects.get_or_create(
                name=translator_name
            )
            publication.translators.add(translator_obj)
            changed = True

        return changed

    def _sync_isbn(
        self,
        publication: BookPublication,
        external_isbn: Optional[str],
        overwrite: bool,
    ) -> set[str]:
        if not external_isbn:
            return set()

        external_isbn = external_isbn.strip()
        if not external_isbn:
            return set()

        isbn13 = external_isbn if len(external_isbn) == 13 else None
        isbn10 = external_isbn if len(external_isbn) == 10 else None

        fields = set()

        if isbn13 and (overwrite or not publication.isbn_13):
            if publication.isbn_13 != isbn13:
                publication.isbn_13 = isbn13
                fields.add("isbn_13")

        if isbn10 and (overwrite or not publication.isbn_10):
            if publication.isbn_10 != isbn10:
                publication.isbn_10 = isbn10
                fields.add("isbn_10")

        return fields

    def _sync_genres(
        self,
        publication: BookPublication,
        categories: Iterable[str],
        overwrite: bool,
    ) -> bool:
        categories = [
            category.strip() for category in categories if category.strip()
        ]
        if not categories:
            return False

        existing_names = {genre.name for genre in publication.genres.all()}
        changed = False

        if overwrite:
            publication.genres.clear()
            existing_names.clear()

        for category in categories:
            genre_obj, _ = Genre.objects.get_or_create(name=category)
            if genre_obj.name in existing_names:
                continue
            publication.genres.add(genre_obj)
            changed = True

        return changed

    def _sync_cover(
        self,
        publication: BookPublication,
        thumbnail_url: Optional[str],
        overwrite: bool,
    ) -> bool:
        if not thumbnail_url:
            return False

        if publication.cover_image and not overwrite:
            return False

        content = self._download_image(thumbnail_url)
        if content is None:
            return False

        path = urlsplit(thumbnail_url).path
        extension = os.path.splitext(path)[1] or ".jpg"
        filename = f"{slugify(publication.title) or 'book'}{extension}"
        publication.cover_image.save(filename, ContentFile(content), save=True)

        return True

    def _download_image(self, url: str) -> Optional[bytes]:
        try:
            response = self.session.get(url, timeout=self.IMAGE_TIMEOUT)
            response.raise_for_status()
            return response.content
        except requests.RequestException:
            logger.debug(
                "Failed to download cover image from %s", url, exc_info=True
            )
            return None

    @staticmethod
    def _parse_publication_date(datetime_str: str) -> Optional[object]:
        """
        Parse ISO 8601 datetime string to date object.
        Format: [YYYY]-[MM]-[DD]T[hh]:[mm]:[ss].000+[tz]
        """
        from datetime import datetime

        if not datetime_str:
            return None

        try:
            # Parse ISO 8601 format and extract date
            dt = datetime.fromisoformat(datetime_str.replace("Z", "+00:00"))
            return dt.date()
        except (ValueError, AttributeError):
            logger.debug("Failed to parse publication date: %s", datetime_str)
            return None


def sync_book_metadata(
    queryset: QuerySet[Book],
    *,
    overwrite: bool = False,
    limit: Optional[int] = None,
) -> SyncResult:
    """
    Convenience function for bulk synchronisation.
    """
    synchronizer = BookMetadataSynchronizer()
    return synchronizer.sync_queryset(
        queryset, overwrite=overwrite, limit=limit
    )
