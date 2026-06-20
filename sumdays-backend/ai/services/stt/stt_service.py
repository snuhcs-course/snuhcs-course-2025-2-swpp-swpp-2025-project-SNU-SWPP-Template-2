import os
import io
from openai import OpenAI

class SpeechToTextService:
    """ Service for converting audio to text using Whisper API. """
    def __init__(self):
        self.client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        self.model = os.getenv("AUDIO_MODEL", "whisper-1")

    def transcribe(self, audio_file, language: str = "ko") -> str:
        """ Transcribe an audio file using OpenAI Whisper API. """
        audio_data = audio_file.read()
        buffer = io.BytesIO(audio_data)
        buffer.name = "file.mp3"

        result = self.client.audio.transcriptions.create(
            model=self.model,
            file=buffer,
            language=language,
            response_format="verbose_json",
        )

        # check if file has no speech
        if hasattr(result, 'segments') and result.segments:
            avg_no_speech_prob = sum(s.no_speech_prob for s in result.segments) / len(result.segments)
            if avg_no_speech_prob > 0.75:
                return ""

        # filter hallucination manually
        temp_filter = [
            "MBC 뉴스",
            "시청해",
            "구독",
            "고맙습니다."
        ]

        for s in temp_filter:
            if s in result.text:
                return ""

        return result.text
