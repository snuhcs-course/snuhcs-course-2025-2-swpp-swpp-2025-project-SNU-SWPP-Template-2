from flask import Blueprint, request, jsonify
from .extract_service import extract_style
from ..image.image_service import ImageService
import json

extract_bp = Blueprint("extract", __name__, url_prefix="/extract")
image_service = ImageService()
MIN_DIARY_NUM = 5

@extract_bp.route("/style", methods=["POST"])
def extract_style_route():
    """ POST http://localhost:5001/extract/style
    Content-Type: multipart/form-data

    diaries(text): ["일기텍스트1", "일기텍스트2", "일기텍스트3"]
    images(file): (업로드한 일기 사진 여러 개)
    """
    diaries = []

    # get text diary input
    if request.form.get("diaries"):
        diaries = json.loads(request.form.get("diaries"))
    elif request.json and request.json.get("diaries"):
        diaries = request.json.get("diaries")

    # get image diary input
    image_files = request.files.getlist("images")
    if image_files:
        ocr_results = image_service.detect_texts_from_diaries(image_files)
        refined_results = ocr_results.get("result", [])

        for diary in refined_results:
            refined_text = diary.get("refined_text", "").strip()
            if refined_text:
                diaries.append(refined_text)

    # check # of diaries
    if len(diaries) < MIN_DIARY_NUM:
        return jsonify({"error": "At least 5 diaries required."}), 400

    result = extract_style(diaries)
    return jsonify(result), 200