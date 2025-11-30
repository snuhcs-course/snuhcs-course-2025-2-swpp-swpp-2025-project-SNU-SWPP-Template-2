import json
import os
import uuid
from datetime import date
from functools import partial
from pathlib import Path
from typing import Annotated

from fastapi import Depends, HTTPException, status
from fastapi.concurrency import run_in_threadpool
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI
from openai import AsyncOpenAI

from app.features.journal.errors import (
    ImageUploadError,
    JournalBadRequestError,
    JournalNotFoundError,
    JournalUpdateError,
)
from app.features.journal.models import Journal, JournalImage, JournalKeyword
from app.features.journal.repository import JournalRepository, S3Repository
from app.features.journal.schemas.requests import (
    ImageCompletionRequest,
    ImageGenerateRequest,
    ImageUploadRequest,
)
from app.features.journal.schemas.responses import (
    JournalKeywordsListResponse,
    PresignedUrlResponse,
)

BASE_PROMPT_PATH = Path(__file__).parent / "prompt"


def _load_prompt(file_name: str) -> str:
    """(Helper) prompts 디렉터리에서 .txt 파일을 읽어옵니다."""
    try:
        with open(BASE_PROMPT_PATH / file_name, encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError as e:
        raise RuntimeError(f"Prompt file not found: {file_name}") from e
    except Exception as e:
        raise RuntimeError(f"Error loading prompt {file_name}: {e}") from e


class JournalService:
    def __init__(
        self,
        journal_repository: Annotated[JournalRepository, Depends()],
        s3_repository: Annotated[S3Repository, Depends()],
    ) -> None:
        self.journal_repository = journal_repository
        self.s3_repository = s3_repository

    def create_journal(
        self,
        user_id: int,
        title: str,
        content: str,
        emotions: dict[str, int],
        gratitude: str | None = None,
    ) -> Journal:
        return self.journal_repository.add_journal(
            user_id=user_id,
            title=title,
            content=content,
            emotions=emotions,
            gratitude=gratitude,
        )

    def get_journal(self, journal_id: int) -> Journal | None:
        journal = self.journal_repository.get_journal_by_id(journal_id)
        if journal is None:
            raise JournalNotFoundError()
        return journal

    def delete_journal(self, journal_id: int) -> None:
        journal_to_delete = self.journal_repository.get_journal_by_id(journal_id)
        self.journal_repository.delete_journal(journal_to_delete)

    def list_journals_by_user(
        self, user_id: int, limit: int, cursor: int | None = None
    ) -> list[Journal]:
        return self.journal_repository.list_journals_by_user(user_id, limit, cursor)

    def update_journal(
        self,
        journal_id: int,
        title: str | None = None,
        content: str | None = None,
        gratitude: str | None = None,
    ) -> None:
        if not any([title, content, gratitude]):
            raise JournalUpdateError()
        journal_to_update = self.journal_repository.get_journal_by_id(journal_id)
        if journal_to_update is None:
            raise JournalNotFoundError(journal_id)
        self.journal_repository.update_journal(
            journal=journal_to_update,
            title=title,
            content=content,
            gratitude=gratitude,
        )

    def get_journal_owner(self, journal_id: int) -> int | None:
        journal = self.journal_repository.get_journal_by_id(journal_id)
        if journal is None:
            return None
        return journal.user_id

    def search_journals(
        self,
        user_id: int,
        title: str | None = None,
        start_date: date | None = None,
        end_date: date | None = None,
        limit: int = 10,
        cursor: int | None = None,
    ) -> list[Journal]:
        if start_date and end_date and start_date > end_date:
            raise JournalBadRequestError("start_date는 end_date보다 이전이어야 합니다.")
        return self.journal_repository.search_journals(
            user_id=user_id,
            title=title,
            start_date=start_date,
            end_date=end_date,
            limit=limit,
            cursor=cursor,
        )

    def get_journals_by_keyword(
        self,
        user_id: int,
        keyword: str,
        limit: int = 10,
        cursor: int | None = None,
    ) -> list[Journal]:
        return self.journal_repository.get_journals_by_keyword(
            user_id=user_id, keyword=keyword, limit=limit, cursor=cursor
        )

    # image upload via S3 presigned URL
    async def create_image_presigned_url(
        self, journal_id: int, payload: ImageUploadRequest
    ) -> PresignedUrlResponse:
        """클라이언트가 이미지 업로드를 시작할 때 호출됩니다."""
        find_journal_task = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal_task)

        if not journal:
            raise JournalNotFoundError(journal_id)

        # 고유 파일 키 생성
        _, file_extension = os.path.splitext(payload.filename)
        unique_filename = f"{uuid.uuid4()}{file_extension}"
        s3_key = f"images/journals/{journal_id}/{unique_filename}"

        # 비동기 S3 작업 실행
        url_data = await self.s3_repository.generate_upload_url(
            s3_key, payload.content_type
        )
        if not url_data:
            raise ImageUploadError("Could not generate S3 presigned URL.")

        return PresignedUrlResponse(**url_data, s3_key=s3_key)

    async def complete_image_upload(
        self, journal_id: int, payload: ImageCompletionRequest
    ) -> JournalImage:
        """클라이언트가 이미지 업로드 완료를 알릴 때 호출됩니다."""
        file_exists = await self.s3_repository.check_file_exists(payload.s3_key)
        if not file_exists:
            raise ImageUploadError("Uploaded image not found in S3.")

        # 저널 존재 확인
        find_journal_task = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal_task)
        if not journal:
            raise JournalNotFoundError(journal_id)

        # 기존 업로드 이미지가 있으면 S3에서 삭제
        existing = await run_in_threadpool(
            self.journal_repository.get_image_by_journal_id,
            journal_id,
        )
        if existing and existing.s3_key:
            # S3에서 삭제
            await self.s3_repository.delete_object(existing.s3_key)

        # DB에서 교체 (replace_journal_image) — image_type='uploaded'
        replace_task = partial(
            self.journal_repository.replace_journal_image,
            journal_id=journal_id,
            existing_image=existing,
            s3_key=payload.s3_key,
        )
        journal_image = await run_in_threadpool(replace_task)
        return journal_image


class JournalOpenAIService:
    """
    OpenAI 관련 로직을 전담하는 서비스
    """

    def __init__(self, journal_repository: JournalRepository = Depends()):
        """
        서비스 초기화 시, 레포지토리 주입 및 OpenAI 비동기 클라이언트 생성
        """
        self.journal_repository = journal_repository

        self.prompt_american_comics = _load_prompt("image_american_comics.txt")
        self.prompt_natural = _load_prompt("image_natural.txt")
        self.prompt_watercolor = _load_prompt("image_watercolor.txt")
        self.prompt_3d_animation = _load_prompt("image_3d_animation.txt")
        self.prompt_pixel_art = _load_prompt("image_pixel_art.txt")

        keyword_prompt_text = _load_prompt("keyword_prompt.txt")

        self.client = AsyncOpenAI()
        if not self.client.api_key:
            raise ValueError("OPENAI_API_KEY not found.")

        self.structured_llm = ChatOpenAI(
            model="gpt-5-mini", temperature=0
        ).with_structured_output(JournalKeywordsListResponse)

        self.keyword_prompt_template = PromptTemplate.from_template(keyword_prompt_text)

    async def request_image_generation(self, request: ImageGenerateRequest) -> str:
        """
        일기를 받아 이미지를 생성하고 Base64로 반환합니다.
        """

        style_prompt_map = {
            "american-comics": self.prompt_american_comics,
            "natural": self.prompt_natural,
            "watercolor": self.prompt_watercolor,
            "3d-animation": self.prompt_3d_animation,
            "pixel-art": self.prompt_pixel_art,
        }

        prompt_template = style_prompt_map.get(request.style)

        try:
            prompt = await self._generate_scene_prompt_from_diary(
                request.content, prompt_template
            )
        except Exception as e:
            print(f"GPT 호출 실패: {e}")

        try:
            image_b64 = await self._generate_image_from_prompt(prompt)
        except Exception as e:
            print(f"DALL-E 3 호출 실패: {e}")

        return image_b64

    async def _generate_scene_prompt_from_diary(
        self, journal_content: str, input_prompt: str
    ) -> str:
        """
        (Helper) GPT-5-mini를 비동기 호출하여 DALL-E용 프롬프트를 생성합니다.
        """
        chat_response = await self.client.chat.completions.create(
            model="gpt-5-mini",
            messages=[
                {"role": "system", "content": input_prompt},
                {"role": "user", "content": journal_content},
            ],
            response_format={"type": "json_object"},
        )
        scene_data = json.loads(chat_response.choices[0].message.content)
        return scene_data["scene_prompt"]

    async def _generate_image_from_prompt(self, prompt: str) -> str:
        """
        (Helper) DALL-E 3를 비동기 호출하여 Base64 이미지 데이터를 반환합니다.
        """
        image_response = await self.client.images.generate(
            model="dall-e-3",
            prompt=prompt,
            size="1024x1024",
            quality="standard",
            n=1,
            response_format="b64_json",
        )
        return image_response.data[0].b64_json

    async def extract_keywords_with_emotion_associations(
        self, journal_id: int
    ) -> list[JournalKeyword]:
        find_journal = partial(
            self.journal_repository.get_journal_by_id, journal_id=journal_id
        )
        journal = await run_in_threadpool(find_journal)
        if not journal:
            raise JournalNotFoundError(journal_id)

        emotion_names = [e.emotion for e in journal.emotions]
        if not emotion_names:
            raise JournalBadRequestError("No emotion in journal")

        chain = self.keyword_prompt_template | self.structured_llm

        input_data = {
            "content": journal.content,
            "emotion_names": ", ".join(emotion_names),
        }
        res = None
        try:
            res_envelope = await chain.ainvoke(input_data)
            res = res_envelope.data
        except Exception as e:
            print(f"LangChain(ainvoke) 호출 실패: {e}")

        if not res:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="LLM이 유효한 키워드를 반환하지 않았습니다.",
            )

        save_task = partial(
            self.journal_repository.add_keywords_emotion_associations,
            journal_id=journal_id,
            keyword_emotion_associations=res,
        )
        created_keywords = await run_in_threadpool(save_task)

        return created_keywords
