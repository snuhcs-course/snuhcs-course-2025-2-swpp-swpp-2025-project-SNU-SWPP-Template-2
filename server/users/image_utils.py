import sqlite3
from urllib.parse import unquote, urlparse
from config.settings import AWS_STORAGE_BUCKET_NAME, S3_CLIENT
import torch
from PIL import Image
from transformers import CLIPProcessor, CLIPModel
from io import BytesIO

# =====================
# Setup
# =====================
device = "cuda" if torch.cuda.is_available() else "cpu"
model_name = "openai/clip-vit-base-patch32"
model = CLIPModel.from_pretrained(model_name).to(device)
processor = CLIPProcessor.from_pretrained(model_name)


def _extract_s3_key(url: str) -> str:
    parsed = urlparse(url)
    return parsed.path.lstrip('/')

def _read_image_from_s3(url: str) -> bytes:
    s3_key = unquote(_extract_s3_key(url))

    response = S3_CLIENT.get_object(
        Bucket=AWS_STORAGE_BUCKET_NAME,
        Key=s3_key
    )

    # 바이트로 읽기
    image_bytes = response['Body'].read()
    return image_bytes

def _get_food_categories() -> list[str]:
    with sqlite3.connect("chroma_db/chroma.sqlite3") as conn:
        cursor = conn.cursor()

        cursor.execute("""
        SELECT string_value AS category
        FROM embedding_metadata
        WHERE key = 'category';
        """)

        categories = [category[0].strip() for category in cursor.fetchall()]
        categories = list(dict.fromkeys(categories))
        return categories


def _predict_category_from_bytes(image_bytes: bytes, categories: list[str]) -> tuple[str, float]:
    """
    Args:
        image_bytes: 이미지 바이트
    Returns:
        (predicted_category, confidence)
    """
    image = Image.open(BytesIO(image_bytes)).convert("RGB")

    texts = [f"a photo of {c}" for c in categories]

    inputs = processor(
        text=texts,
        images=[image],
        return_tensors="pt",
        padding=True
    ).to(device)

    with torch.no_grad():
        outputs = model(**inputs)
        logits_per_image = outputs.logits_per_image
        probs = logits_per_image.softmax(dim=1)

    best_idx = probs[0].argmax().item()
    return categories[best_idx], probs[0][best_idx].item()

def get_food_image_category(url: str) -> tuple[str, float]:
    image_bytes = _read_image_from_s3(url)
    food_categories = _get_food_categories()
    return _predict_category_from_bytes(image_bytes, food_categories)