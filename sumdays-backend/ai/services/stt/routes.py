from flask import Blueprint, request, jsonify
from .stt_service import SpeechToTextService

stt_service = SpeechToTextService()
stt_bp = Blueprint("stt", __name__, url_prefix="/stt")

@stt_bp.route("/memo", methods=["POST"])
def stt_memo():
    """POST http://localhost:5001/stt/memo
    POSTMAN form-data 
    audio file | {test}.wav
    """
    try:
        if "audio" not in request.files:
            return jsonify({"error": "No audio file uploaded."}), 400

        audio_file = request.files["audio"]
        text = stt_service.transcribe(audio_file)

        return jsonify({"transcribed_text": text}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 400