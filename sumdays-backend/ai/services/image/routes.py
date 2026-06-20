from flask import Blueprint, request, jsonify
from .image_service import ImageService

image_service = ImageService()
image_bp = Blueprint("image", __name__, url_prefix="/image")

@image_bp.route("/memo", methods=["POST"])
def image_memo():
    """POST http://localhost:5001/image/memo
    multipart/form-data
    - image: file
    - type: extract | describe
    """
    try:
        if "image" not in request.files:
            return jsonify({"error": "No image file uploaded."}), 400
        
        analysis_type = request.form.get("type", "").lower()
        if not (analysis_type=="extract" or analysis_type=="describe"):
            return jsonify({"error": "Invalid analysis type."}), 400
        
        image_file = request.files["image"]
        response = image_service.analyze(image_file, analysis_type)
        
        return jsonify(response), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 400

@image_bp.route("/diary", methods=["POST"])
def image_diary():
    """POST http://localhost:5001/image/diary
    multipart/form-data
    - image: multiple files
    """
    try:
        image_files = request.files.getlist("image")
        if not image_files:
            return jsonify({"error": "No image files uploaded."}), 400
        
        response = image_service.detect_texts_from_diaries(image_files)
        
        return jsonify(response), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 400