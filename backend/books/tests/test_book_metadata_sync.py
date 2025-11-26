"""
Tests for the book metadata synchronisation pipeline.
"""

from __future__ import annotations

import shutil
import tempfile
from unittest.mock import MagicMock, patch

from books.models import BookCopy, BookPublication
from books.services.book_metadata_sync import (
    BookMetadataSynchronizer,
    _SimpleHTMLParser,
)
from books.services.kakao_book_pipeline import (
    ExternalBook,
    ExternalBookAPIError,
)
from django.contrib.auth import get_user_model
from django.test import TestCase, override_settings

User = get_user_model()

SAMPLE_PNG = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01"
    b"\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\nIDATx\x9cc``\x00\x00\x00"
    b"\x02\x00\x01\xe2!\xbc3\x00\x00\x00\x00IEND\xaeB`\x82"
)


class BookMetadataSynchronizerTestCase(TestCase):
    """
    Validate the behaviour of the BookMetadataSynchronizer service.
    """

    def setUp(self):
        self.media_root = tempfile.mkdtemp()
        self.override = override_settings(MEDIA_ROOT=self.media_root)
        self.override.enable()

        self.addCleanup(self.override.disable)
        self.addCleanup(
            lambda: shutil.rmtree(self.media_root, ignore_errors=True)
        )

        self.owner = User.objects.create_user(
            username="owner",
            email="owner@example.com",
            password="pass1234",
        )

        self.publication = BookPublication.objects.create(
            title="The Testing Book",
        )
        self.book_copy = BookCopy.objects.create(
            publication=self.publication,
            owner=self.owner,
        )

    def _build_synchronizer(self, pipeline):
        synchronizer = BookMetadataSynchronizer(
            pipeline=pipeline,
            session=MagicMock(),
        )
        synchronizer._download_image = MagicMock(return_value=SAMPLE_PNG)
        synchronizer._fetch_external_description = MagicMock(return_value=None)
        return synchronizer

    def _build_metadata(self, **overrides):
        data = {
            "title": "The Testing Book",
            "authors": ["Jane Author"],
            "translators": [],
            "categories": ["Testing"],
            "description": "Short preview",
            "thumbnail_url": "http://example.com/cover.jpg",
            "isbn": "9781234567890",
            "publisher": "Quality Press",
            "url": "http://example.com/book",
            "publication_date": "2024-01-01T00:00:00",
            "price": 10000,
            "sale_price": 9000,
            "status": "정상",
        }
        data.update(overrides)
        return ExternalBook(**data)

    @patch(
        "books.services.book_metadata_sync.slugify",
        lambda value: "the-testing-book",
    )
    def test_sync_book_populates_missing_metadata(self):
        """
        The synchroniser should fill in description, authors, genres, ISBN and cover.
        """
        candidate = self._build_metadata(
            categories=["Testing", "Technology"],
            description="A definitive guide to testing pipelines.",
        )

        pipeline = MagicMock()
        pipeline.fetch.return_value = [candidate]

        synchronizer = self._build_synchronizer(pipeline)

        changed = synchronizer.sync_book(self.publication)
        self.assertTrue(changed)

        self.publication.refresh_from_db()
        self.assertEqual(self.publication.description, candidate.description)
        self.assertEqual(self.publication.isbn_13, candidate.isbn)
        self.assertEqual(self.publication.publisher.name, candidate.publisher)
        self.assertEqual(self.publication.authors.count(), 1)
        self.assertEqual(
            self.publication.authors.first().name,
            "Jane Author",
        )
        self.assertGreater(self.publication.genres.count(), 0)
        self.assertTrue(self.publication.cover_image.name.endswith(".jpg"))

    def test_sync_book_skips_when_no_query_available(self):
        """
        Books without title and ISBN should be skipped.
        """
        book = BookPublication.objects.create(title="")
        pipeline = MagicMock()

        synchronizer = self._build_synchronizer(pipeline)

        changed = synchronizer.sync_book(book)
        self.assertFalse(changed)
        pipeline.fetch.assert_not_called()

    def test_sync_book_propagates_external_errors(self):
        """
        Underlying API failures should surface so callers can handle them.
        """
        pipeline = MagicMock()
        pipeline.fetch.side_effect = ExternalBookAPIError()

        synchronizer = self._build_synchronizer(pipeline)

        with self.assertRaises(ExternalBookAPIError):
            synchronizer.sync_book(self.publication)

    def test_sync_queryset_counts_failures(self):
        """
        Batch synchronisation should count processed, updated, and failed books.
        """
        second_publication = BookPublication.objects.create(
            title="Another Testing Book",
        )
        BookCopy.objects.create(
            publication=second_publication,
            owner=self.owner,
        )

        first_candidate = self._build_metadata(
            description="desc",
            thumbnail_url=None,
            publisher=None,
        )

        pipeline = MagicMock()
        pipeline.fetch.side_effect = [
            [first_candidate],
            ExternalBookAPIError(),
        ]

        synchronizer = self._build_synchronizer(pipeline)

        summary = synchronizer.sync_queryset(
            BookPublication.objects.all(), limit=2
        )

        self.assertEqual(summary.processed, 2)
        self.assertEqual(summary.updated, 1)
        self.assertEqual(summary.failures, 1)

    def test_full_description_overrides_snippet(self):
        candidate = self._build_metadata(description="Short preview...")

        pipeline = MagicMock()
        pipeline.fetch.return_value = [candidate]

        synchronizer = self._build_synchronizer(pipeline)
        synchronizer._fetch_external_description.return_value = (
            "Long form description from detail page."
        )

        changed = synchronizer.sync_book(self.publication)
        self.assertTrue(changed)
        self.publication.refresh_from_db()
        self.assertEqual(
            self.publication.description,
            "Long form description from detail page.",
        )

    def test_extract_description_from_dom_parser(self):
        synchronizer = BookMetadataSynchronizer(
            pipeline=MagicMock(),
            session=MagicMock(),
        )
        html = """
        <div id="tabContent">
            <div>
                <div></div>
                <div></div>
                <div>
                    <p>Full body text</p>
                </div>
            </div>
        </div>
        """
        parser = _SimpleHTMLParser()
        parser.feed(html)
        parser.close()

        extracted = synchronizer._extract_description_from_dom(parser.root)
        self.assertEqual(extracted, "Full body text")

    def test_isbn_conflict_is_skipped(self):
        other_publication = BookPublication.objects.create(
            title="Existing ISBN",
            isbn_13="9787522518954",
        )

        target_publication = BookPublication.objects.create(
            title="Needs ISBN",
        )

        candidate = self._build_metadata(
            isbn="9787522518954",
            thumbnail_url=None,
        )

        pipeline = MagicMock()
        pipeline.fetch.return_value = [candidate]

        synchronizer = self._build_synchronizer(pipeline)
        changed = synchronizer.sync_book(target_publication, overwrite=True)

        target_publication.refresh_from_db()
        self.assertTrue(changed)  # other metadata (description) still applied
        self.assertIsNone(target_publication.isbn_13)
        self.assertEqual(other_publication.isbn_13, "9787522518954")

    def test_sync_book_applies_genre_and_author(self):
        """Test that genre and author are correctly applied from metadata."""
        candidate = self._build_metadata(
            categories=["Testing", "Python"],
            authors=["Jane Author", "John Tester"],
        )
        pipeline = MagicMock()
        pipeline.fetch.return_value = [candidate]
        synchronizer = self._build_synchronizer(pipeline)
        changed = synchronizer.sync_book(self.publication)
        self.assertTrue(changed)
        self.publication.refresh_from_db()
        genres = [g.name for g in self.publication.genres.all()]
        self.assertIn("Testing", genres)
        self.assertIn("Python", genres)
        authors = [a.name for a in self.publication.authors.all()]
        self.assertIn("Jane Author", authors)
        self.assertIn("John Tester", authors)

    def test_sync_book_handles_missing_thumbnail(self):
        """Test that missing thumbnail does not break sync."""
        candidate = self._build_metadata(thumbnail_url=None)
        pipeline = MagicMock()
        pipeline.fetch.return_value = [candidate]
        synchronizer = self._build_synchronizer(pipeline)
        changed = synchronizer.sync_book(self.publication)
        self.assertTrue(changed)
        self.publication.refresh_from_db()
        self.assertTrue(self.publication.cover_image.name.endswith(".jpg") or self.publication.cover_image.name == "")

    def test_sync_book_conflict_isbn(self):
        """Test that ISBN conflict skips ISBN update but applies other metadata."""
        other_pub = BookPublication.objects.create(title="Other", isbn_13="9789999999999")
        target_pub = BookPublication.objects.create(title="Target")
        candidate = self._build_metadata(isbn="9789999999999", description="desc")
        pipeline = MagicMock()
        pipeline.fetch.return_value = [candidate]
        synchronizer = self._build_synchronizer(pipeline)
        changed = synchronizer.sync_book(target_pub, overwrite=True)
        target_pub.refresh_from_db()
        self.assertTrue(changed)
        self.assertIsNone(target_pub.isbn_13)
        self.assertEqual(other_pub.isbn_13, "9789999999999")
        self.assertEqual(target_pub.description, "desc")
