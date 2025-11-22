import re
from typing import Annotated

from pydantic import AfterValidator, BaseModel

from app.common.errors import InvalidFieldFormatError

URL_PATTERN = re.compile(
    r"(https?://)?(www.)?[-a-zA-Z0-9@:%.+~#=]{2,256}.[a-z]{2,6}\b([-a-zA-Z0-9@:%+.~#?&//=]*)"
)

ALLOWED_IMAGE_TYPES = {
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp",
}

ALLOWED_EMOTIONS = {
    "happy",
    "sad",
    "anxious",
    "calm",
    "annoyed",
    "satisfied",
    "bored",
    "interested",
    "lethargic",
    "energetic",
}

ALLOWD_STYLE = {
    "natural",
    "american-comics",
    "watercolor",
    "3d-animation",
    "pixel-art",
}


def validate_title(title: str) -> str:
    if not title.strip():
        raise InvalidFieldFormatError("title")
    if len(title) > 255:
        raise InvalidFieldFormatError("title")
    return title


def validate_content(content: str) -> str:
    if not content.strip():
        raise InvalidFieldFormatError("content")
    return content


def validate_url(value: list[str] | None) -> list[str] | None:
    if value is None:
        return value
    for url in value:
        if not re.match(URL_PATTERN, url):
            raise InvalidFieldFormatError("url")
    return value


def validate_filename(filename: str) -> str:
    stripped_filename = filename.strip()
    if not stripped_filename:
        raise InvalidFieldFormatError("filename cannot be empty")

    if len(stripped_filename) > 255:
        raise InvalidFieldFormatError("filename is too long")

    if "/" in stripped_filename or "\\" in stripped_filename:
        raise InvalidFieldFormatError("filename cannot contain path separators")

    if "." not in stripped_filename or stripped_filename.startswith("."):
        raise InvalidFieldFormatError("filename must have a valid extension")

    return stripped_filename


def validate_content_type(content_type: str) -> str:
    normalized_type = content_type.strip().lower()
    if normalized_type not in ALLOWED_IMAGE_TYPES:
        raise InvalidFieldFormatError(
            f"Unsupported content_type. Allowed types are: {', '.join(ALLOWED_IMAGE_TYPES)}"
        )
    return normalized_type


def validate_emotions(value: dict[str, int]) -> dict[str, int]:
    # 감정 키(key)가 허용된 10가지 종류인지 확인
    if not set(value.keys()).issubset(ALLOWED_EMOTIONS):
        raise ValueError("Invalid emotion keys provided.")

    # 감정 강도(intensity)가 유효한 범위(0-4)인지 확인
    for intensity in value.values():
        if not 0 <= intensity <= 4:  # 강도 범위를 0-4로 가정
            raise ValueError("Emotion intensity must be between 0 and 4.")

    return value


def validate_style(value: str) -> str:
    if value in ALLOWD_STYLE:
        return value
    else:
        raise ValueError(
            "Invalid style request: choose one in [natural, american-comics, abstract, impressionism-gogh]"
        )


class JournalCreateRequest(BaseModel):
    title: Annotated[str, AfterValidator(validate_title)]
    content: Annotated[str, AfterValidator(validate_content)]
    emotions: Annotated[dict[str, int], AfterValidator(validate_emotions)]
    gratitude: str | None = None


class JournalUpdateRequest(BaseModel):
    title: Annotated[str | None, AfterValidator(validate_title)] = None
    content: Annotated[str | None, AfterValidator(validate_content)] = None
    gratitude: str | None = None


class ImageUploadRequest(BaseModel):
    """클라이언트가 이미지 업로드를 요청할 때 보내는 데이터"""

    filename: Annotated[str, AfterValidator(validate_filename)]
    content_type: Annotated[str, AfterValidator(validate_content_type)]


class ImageCompletionRequest(BaseModel):
    """ "클라이언트가 이미지 업로드 완료를 알릴 때 보내는 데이터"""

    s3_key: str


class ImageGenerateRequest(BaseModel):
    """클라이언트가 이미지 생성을 요청할 때 보내는 데이터"""

    style: Annotated[str, AfterValidator(validate_style)]
    content: Annotated[str, AfterValidator(validate_content)]
