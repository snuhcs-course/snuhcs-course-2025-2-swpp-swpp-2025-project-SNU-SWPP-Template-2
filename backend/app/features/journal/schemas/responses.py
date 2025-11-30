from datetime import datetime

from pydantic import BaseModel, Field

from app.features.journal.models import Journal, JournalImage, JournalKeyword


class KeywordEmotionAssociationItem(BaseModel):
    keyword: str
    emotion: str
    summary: str
    weight: float = Field(..., ge=0.0, le=1.0, description="association (0..1)")

    @staticmethod
    def from_journal_keyword(
        journal_keyword: JournalKeyword,
    ) -> "KeywordEmotionAssociationItem":
        return KeywordEmotionAssociationItem(
            keyword=journal_keyword.keyword,
            emotion=journal_keyword.emotion,
            summary=journal_keyword.summary,
            weight=journal_keyword.weight,
        )


class JournalKeywordsListResponse(BaseModel):
    data: list[KeywordEmotionAssociationItem]

    @staticmethod
    def from_journal_keywords(
        keywords_list: list[JournalKeyword],
    ) -> "JournalKeywordsListResponse":
        return JournalKeywordsListResponse(
            data=[
                KeywordEmotionAssociationItem.from_journal_keyword(kw)
                for kw in keywords_list
            ]
        )


class JournalEmotionResponse(BaseModel):
    emotion: str
    intensity: int


class JournalResponse(BaseModel):
    id: int
    title: str
    content: str
    emotions: list[JournalEmotionResponse]
    keywords: list[KeywordEmotionAssociationItem]
    image_s3_keys: str | None = None
    gratitude: str | None = None
    created_at: datetime

    @staticmethod
    def from_journal(journal: Journal) -> "JournalResponse":
        return JournalResponse(
            id=journal.id,
            title=journal.title,
            content=journal.content,
            emotions=[
                JournalEmotionResponse(
                    emotion=emotion.emotion, intensity=emotion.intensity
                )
                for emotion in journal.emotions
            ],
            keywords=[
                KeywordEmotionAssociationItem.from_journal_keyword(keyword)
                for keyword in journal.keywords
            ],
            image_s3_keys=journal.image.s3_key if journal.image else None,
            gratitude=journal.gratitude,
            created_at=journal.created_at,
        )


class JournalCursorResponse(BaseModel):
    items: list[JournalResponse]
    next_cursor: int | None = Field(
        None, description="다음 페이지를 요청할 때 사용할 마지막 아이템의 ID"
    )

    @staticmethod
    def from_journals(journals: list[Journal], limit: int) -> "JournalCursorResponse":
        items = [JournalResponse.from_journal(journal) for journal in journals]
        next_cursor = None
        if items and len(items) == limit:
            next_cursor = items[-1].id
        return JournalCursorResponse(items=items, next_cursor=next_cursor)


class JournalListResponse(BaseModel):
    data: list[JournalResponse]

    @staticmethod
    def from_journals(journals: list[Journal]) -> "JournalListResponse":
        items = [JournalResponse.from_journal(journal) for journal in journals]
        return JournalListResponse(data=items)


class PresignedUrlResponse(BaseModel):
    presigned_url: str
    file_url: str
    s3_key: str


class JournalImageResponse(BaseModel):
    id: int
    journal_id: int
    s3_key: str
    created_at: datetime

    @staticmethod
    def from_journal_image(journal_image: JournalImage) -> "JournalImageResponse":
        return JournalImageResponse(
            id=journal_image.id,
            journal_id=journal_image.journal_id,
            s3_key=journal_image.s3_key,
            created_at=journal_image.created_at,
        )


class ImageGenerateResponse(BaseModel):
    """이미지 생성 요청 후 클라이언트에게 반환하는 데이터:"""

    image_base64: str
