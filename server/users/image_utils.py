import logging
import os
from urllib.parse import unquote, urlparse
from config.settings import AWS_STORAGE_BUCKET_NAME, S3_CLIENT
import torch
from PIL import Image
from transformers import CLIPProcessor, CLIPModel
from io import BytesIO
from .foodlist_matcher import FoodListMatcher

logger = logging.getLogger(__name__)

# =====================
# Setup
# =====================
ENABLE_CLIP_LABELING = os.getenv('ENABLE_CLIP_LABELING', 'true').lower() == 'true'

device = "cuda" if torch.cuda.is_available() else "cpu"
model = None
processor = None
foodlist_matcher = None

if ENABLE_CLIP_LABELING:
    try:
        model_name = "openai/clip-vit-base-patch32"
        model = CLIPModel.from_pretrained(model_name).to(device)
        processor = CLIPProcessor.from_pretrained(model_name)
        foodlist_matcher = FoodListMatcher()
        logger.info("CLIP model and food list matcher initialized successfully")
    except Exception as e:
        logger.warning(f"Failed to initialize CLIP model: {e}. CLIP labeling will be disabled.")
        ENABLE_CLIP_LABELING = False
else:
    logger.info("CLIP labeling is disabled")


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
    """Get food categories from foodlist_matcher"""
    if foodlist_matcher is None:
        logger.warning("FoodListMatcher not initialized, returning empty categories")
        return []

    try:
        # Get all unique food names from foodlist as categories
        categories = foodlist_matcher.food_names
        return categories if categories else []
    except Exception as e:
        logger.error(f"Error getting food categories: {e}")
        return []


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
    """Legacy function: returns top 1 category"""
    image_bytes = _read_image_from_s3(url)
    food_categories = _get_food_categories()
    return _predict_category_from_bytes(image_bytes, food_categories)


def get_food_image_with_alternatives_from_bytes(image_bytes: bytes) -> dict:
    """
    Get food category with top-5 alternatives from foodlist.json using raw image bytes

    Returns:
        {
            'primary_label': '치킨',
            'confidence': 0.95,
            'alternatives': [
                {'name': '닭다리', 'confidence': 0.85},
                {'name': '튀김', 'confidence': 0.75},
                ...
            ],
            'clip_prediction': '치킨',
            'clip_confidence': 0.92
        }
    """
    # If CLIP labeling is disabled, return empty result
    if not ENABLE_CLIP_LABELING or model is None or processor is None or foodlist_matcher is None:
        logger.debug("CLIP labeling is disabled")
        return {
            'primary_label': '',
            'confidence': 0.0,
            'alternatives': [],
            'error': 'CLIP labeling is disabled'
        }

    try:
        # Open image from bytes
        food_categories = _get_food_categories()

        # Get top category from CLIP
        predicted_label, confidence = _predict_category_from_bytes(image_bytes, food_categories)

        # Find best matches from foodlist.json
        alternatives = foodlist_matcher.find_best_matches(predicted_label, top_k=5)

        logger.info(f"Categorized image: primary={alternatives[0]['name']}, confidence={alternatives[0]['confidence']:.2f}")

        return {
            'primary_label': alternatives[0]['name'],  # Best match from official list
            'confidence': alternatives[0]['confidence'],
            'alternatives': alternatives[1:],  # Next 4 alternatives
            'clip_prediction': predicted_label,  # Original CLIP output (for debug)
            'clip_confidence': confidence  # CLIP confidence (for debug)
        }
    except Exception as e:
        logger.error(f"Error in get_food_image_with_alternatives_from_bytes: {e}")
        return {
            'primary_label': '',
            'confidence': 0.0,
            'alternatives': [],
            'error': str(e)
        }


def get_food_image_with_alternatives(url: str) -> dict:
    """
    Get food category with top-5 alternatives from foodlist.json

    Returns:
        {
            'primary_label': '치킨',
            'confidence': 0.95,
            'alternatives': [
                {'name': '닭다리', 'confidence': 0.85},
                {'name': '튀김', 'confidence': 0.75},
                ...
            ],
            'clip_prediction': '치킨',  # Original CLIP output
            'clip_confidence': 0.92     # CLIP confidence
        }
    """
    # If CLIP labeling is disabled, return empty result
    if not ENABLE_CLIP_LABELING or model is None or processor is None or foodlist_matcher is None:
        logger.debug("CLIP labeling is disabled")
        return {
            'primary_label': '',
            'confidence': 0.0,
            'alternatives': [],
            'error': 'CLIP labeling is disabled'
        }

    try:
        image_bytes = _read_image_from_s3(url)
        food_categories = _get_food_categories()

        # Get top category from CLIP
        predicted_label, confidence = _predict_category_from_bytes(image_bytes, food_categories)

        # Find best matches from foodlist.json
        alternatives = foodlist_matcher.find_best_matches(predicted_label, top_k=5)

        logger.info(f"Categorized image: primary={alternatives[0]['name']}, confidence={alternatives[0]['confidence']:.2f}")

        return {
            'primary_label': alternatives[0]['name'],  # Best match from official list
            'confidence': alternatives[0]['confidence'],
            'alternatives': alternatives[1:],  # Next 4 alternatives
            'clip_prediction': predicted_label,  # Original CLIP output (for debug)
            'clip_confidence': confidence  # CLIP confidence (for debug)
        }
    except Exception as e:
        logger.error(f"Error in get_food_image_with_alternatives: {e}")
        return {
            'primary_label': '',
            'confidence': 0.0,
            'alternatives': [],
            'error': str(e)
        }