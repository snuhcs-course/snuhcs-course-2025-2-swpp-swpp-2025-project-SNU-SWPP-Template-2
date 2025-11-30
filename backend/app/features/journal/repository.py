from datetime import date, datetime, timedelta
from typing import IO, Annotated

import boto3
from botocore.exceptions import ClientError
from fastapi import Depends
from fastapi.concurrency import run_in_threadpool
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.database.session import get_db_session
from app.features.journal.schemas.responses import KeywordEmotionAssociationItem

from .models import Journal, JournalEmotion, JournalImage, JournalKeyword


class JournalRepository:
    def __init__(self, session: Annotated[Session, Depends(get_db_session)]) -> None:
        self.session = session

    def add_journal(
        self,
        user_id: int,
        title: str,
        content: str,
        emotions: dict[str, int],
        gratitude: str | None = None,
    ) -> Journal:
        journal = Journal(
            user_id=user_id,
            title=title,
            content=content,
        )

        for emotion_name, intensity_value in emotions.items():
            emotions_to_save = JournalEmotion(
                emotion=emotion_name, intensity=intensity_value
            )
            journal.emotions.append(emotions_to_save)

        if gratitude:
            journal.gratitude = gratitude

        self.session.add(journal)
        self.session.flush()
        return journal

    def get_journal_by_id(self, journal_id: int) -> Journal | None:
        return self.session.get(Journal, journal_id)

    def delete_journal(self, journal: Journal) -> None:
        self.session.delete(journal)

    def list_journals_by_user(
        self, user_id: int, limit: int = 10, cursor: int | None = None
    ) -> list[Journal]:
        # cursor가 None이면 최신 글부터, cursor가 주어지면 해당 ID보다 작은 글부터
        query = (
            self.session.query(Journal)
            .filter(Journal.user_id == user_id)
            .order_by(Journal.id.desc())
        )
        # cursor가 주어지면 해당 ID보다 작은 글부터
        if cursor is not None:
            query = query.filter(Journal.id < cursor)
        # limit만큼 가져오기
        return query.limit(limit).all()

    def update_journal(
        self,
        journal: Journal,
        title: str | None = None,
        content: str | None = None,
        gratitude: str | None = None,
    ) -> None:
        if title is not None:
            journal.title = title
        if content is not None:
            journal.content = content
        if gratitude is not None:
            journal.gratitude = gratitude
        self.session.flush()

    def search_journals(
        self,
        user_id: int,
        title: str | None = None,
        start_date: date | None = None,
        end_date: date | None = None,
        limit: int = 10,
        cursor: int | None = None,
    ) -> list[Journal]:
        query = self.session.query(Journal).filter(Journal.user_id == user_id)

        if title:
            query = query.filter(Journal.title.ilike(f"%{title}%"))
        if start_date:
            query = query.filter(
                Journal.created_at >= datetime.combine(start_date, datetime.min.time())
            )
        if end_date:
            query = query.filter(
                Journal.created_at
                < datetime.combine(end_date + timedelta(days=1), datetime.min.time())
            )

        if cursor is not None:
            query = query.filter(Journal.id < cursor)

        query = query.order_by(Journal.id.desc())

        return query.limit(limit).all()

    def add_keywords_emotion_associations(
        self,
        journal_id: int,
        keyword_emotion_associations: list[KeywordEmotionAssociationItem],
    ) -> list[JournalKeyword]:
        # delete the existing keywords list
        journal = self.get_journal_by_id(journal_id)
        if journal:
            self.drop_journal_keywords(journal_id)
        journal_keyword_list = []
        for entry in keyword_emotion_associations:
            journal_keyword = JournalKeyword(
                journal_id=journal_id,
                keyword=entry.keyword,
                emotion=entry.emotion,
                summary=entry.summary,
                weight=entry.weight,
            )
            self.session.add(journal_keyword)
            journal_keyword_list.append(journal_keyword)
        self.session.flush()
        return journal_keyword_list

    def get_journals_by_keyword(
        self,
        user_id: int,
        keyword: str,
        limit: int = 10,
        cursor: int | None = None,
    ) -> list[Journal]:
        stmt = select(Journal).where(
            Journal.user_id == user_id,
            Journal.keywords.any(JournalKeyword.keyword == keyword),
        )
        if cursor is not None:
            stmt = stmt.where(Journal.id < cursor)
        stmt = stmt.order_by(Journal.id.desc()).limit(limit)
        return self.session.execute(stmt).scalars().all()

    def drop_journal_keywords(
        self,
        journal_id: int,
    ):
        journal = self.get_journal_by_id(journal_id)
        journal.keywords = []

    def get_image_by_journal_id(self, journal_id: int) -> JournalImage | None:
        return (
            self.session.query(JournalImage)
            .filter(
                JournalImage.journal_id == journal_id,
            )
            .first()
        )

    def delete_journal_image(self, journal_image: JournalImage) -> None:
        if journal_image:
            self.session.delete(journal_image)
            self.session.flush()

    def replace_journal_image(
        self,
        journal_id: int,
        existing_image: JournalImage | None = None,
        s3_key: str | None = None,
    ) -> JournalImage:
        """
        같은 journal_id의 기존 이미지를 삭제하고 새 레코드 생성.
        기존 이미지가 없다면 신규 이미지 레코드 생성.
        반환값은 새로 생성된 JournalImage 객체.
        """
        if existing_image:
            # DB에서 삭제
            self.session.delete(existing_image)
            self.session.flush()

        new_image = JournalImage(
            journal_id=journal_id,
            s3_key=s3_key,
        )
        self.session.add(new_image)
        self.session.flush()
        return new_image


class S3Repository:
    def __init__(self):
        self.s3_client = boto3.client(
            "s3",
            aws_access_key_id=settings.AWS_ACCESS_KEY_ID,
            aws_secret_access_key=settings.AWS_SECRET_ACCESS_KEY,
            region_name=settings.AWS_REGION,
        )
        self.bucket_name = settings.AWS_S3_BUCKET_NAME

    async def generate_upload_url(self, s3_key: str, content_type: str) -> dict | None:
        try:
            presigned_url = await run_in_threadpool(
                self.s3_client.generate_presigned_url,
                ClientMethod="put_object",
                Params={
                    "Bucket": self.bucket_name,
                    "Key": s3_key,
                    "ContentType": content_type,
                },
                ExpiresIn=3600,
            )

            final_file_url = f"https://{self.bucket_name}.s3.{settings.AWS_REGION}.amazonaws.com/{s3_key}"
            return {"presigned_url": presigned_url, "file_url": final_file_url}
        except ClientError:
            return None

    async def check_file_exists(self, s3_key: str) -> bool:
        try:
            await run_in_threadpool(
                self.s3_client.head_object, Bucket=self.bucket_name, Key=s3_key
            )
            return True
        except ClientError as e:
            if e.response["Error"]["Code"] == "404":
                # File does not exist in S3
                return False
            else:
                raise e

    async def upload_file_object(
        self, file_obj: IO[bytes], s3_key: str, content_type: str
    ) -> dict | None:
        try:
            await run_in_threadpool(
                self.s3_client.upload_fileobj,
                Fileobj=file_obj,
                Bucket=self.bucket_name,
                Key=s3_key,
                ExtraArgs={"ContentType": content_type},
            )
            file_url = f"https://{self.bucket_name}.s3.{settings.AWS_REGION}.amazonaws.com/{s3_key}"
            return {"file_url": file_url, "s3_key": s3_key}
        except ClientError:
            return None

    async def delete_object(self, s3_key: str) -> bool:
        try:
            await run_in_threadpool(
                self.s3_client.delete_object, Bucket=self.bucket_name, Key=s3_key
            )
            return True
        except ClientError:
            return False
