import io
import pytest
from unittest.mock import patch, MagicMock

def create_mock_response(text, no_speech_prob=0.0):
    mock_res = MagicMock()
    mock_res.text = text
    
    segment = MagicMock()
    segment.no_speech_prob = no_speech_prob
    mock_res.segments = [segment]
    
    return mock_res

LIBRARY_PATCH_PATH = "openai.resources.audio.Transcriptions.create"

def test_stt_memo_success(client):
    fake_audio = (io.BytesIO(b"valid-audio"), "test.wav")
    data = {"audio": fake_audio}

    with patch(LIBRARY_PATCH_PATH) as mock_create:
        mock_create.return_value = create_mock_response(
            text="성공했습니다", 
            no_speech_prob=0.1
        )

        res = client.post("/stt/memo", data=data, content_type="multipart/form-data")
        
        if res.status_code != 200:
            print(f"DEBUG ERROR: {res.get_data(as_text=True)}")

        res_json = res.get_json()

        assert res.status_code == 200
        assert res_json["transcribed_text"] == "성공했습니다"


def test_stt_memo_silence(client):
    fake_audio = (io.BytesIO(b"silence"), "silence.wav")
    data = {"audio": fake_audio}

    with patch(LIBRARY_PATCH_PATH) as mock_create:
        mock_create.return_value = create_mock_response(
            text="...", 
            no_speech_prob=0.8  
        )

        res = client.post("/stt/memo", data=data, content_type="multipart/form-data")
        res_json = res.get_json()

        assert res.status_code == 200
        assert res_json["transcribed_text"] == ""


def test_stt_memo_hallucination(client):
    fake_audio = (io.BytesIO(b"news"), "news.wav")
    data = {"audio": fake_audio}

    with patch(LIBRARY_PATCH_PATH) as mock_create:
        mock_create.return_value = create_mock_response(
            text="이것은 MBC 뉴스입니다.", 
            no_speech_prob=0.1
        )

        res = client.post("/stt/memo", data=data, content_type="multipart/form-data")
        res_json = res.get_json()

        assert res.status_code == 200
        assert res_json["transcribed_text"] == ""


def test_stt_memo_exception(client):
    fake_audio = (io.BytesIO(b"error"), "error.wav")
    data = {"audio": fake_audio}

    with patch(LIBRARY_PATCH_PATH) as mock_create:
        mock_create.side_effect = Exception("OpenAI API Failure")

        res = client.post("/stt/memo", data=data, content_type="multipart/form-data")
        res_json = res.get_json()

        assert res.status_code == 400
        assert "OpenAI API Failure" in res_json["error"]