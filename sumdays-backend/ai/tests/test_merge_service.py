import json
import numpy as np
import pytest

from ai.services.merge.merge_service import (
    l2norm,
    embed_sentences,
    choose_best_sentence,
    count_sentences,
    merge_rerank,
)

@pytest.fixture(autouse=True)
def mock_all(monkeypatch):
    class FakeAnalysis:
        def analyze(self, diary):
            return {
                "emoji": "🙂",
                "feedback": "좋았던 하루였습니다.",
                "keywords": ["빵", "점심", "가족"],
                "emotion_score": 0.7,
            }

    import ai.services.merge.routes as merge_routes
    monkeypatch.setattr(
        merge_routes.analysis_service, "analyze", FakeAnalysis().analyze
    )

    import ai.services.merge.merge_service as ms

    class FakeMessage:
        def __init__(self, content):
            self.content = content

    class FakeChoice:
        def __init__(self, content):
            self.message = FakeMessage(content)

    class FakeResponse:
        def __init__(self, content):
            self.choices = [FakeChoice(content)]

    def fake_create(*args, **kwargs):
        return FakeResponse("첫번째 단락입니다.\n###\n두번째 단락입니다.")

    monkeypatch.setattr(
        ms.client.chat.completions, "create", fake_create
    )


def _build_base_payload(end_flag: bool, advanced_flag: bool = False) -> dict:
    return {
        "memos": [
            {"id": 1, "content": "아침으로 빵을 먹었다.", "order": 1},
            {"id": 3, "content": "점심은 친구와 맛있게 먹었다.", "order": 2},
            {"id": 2, "content": "저녁을 가족과 먹었다.", "order": 3},
        ],
        "end_flag": end_flag,
        "advanced_flag": advanced_flag,
        "temperature": 0.8,
        "style_prompt": {
            "common_phrases": ["그냥 그랬다", "피곤했다"],
            "emotional_tone": "일상적이고 약간 피곤한 느낌",
        },
        "style_examples": [
            "오늘도 그냥 그런 하루였다.",
            "피곤하지만 그래도 할 일은 했다.",
        ],
        "style_vector": [1.0, 0.0, 0.0],
    }



def test_merge_only_streaming_route(client):
    payload = _build_base_payload(end_flag=False, advanced_flag=False)

    res = client.post("/merge/", data=json.dumps(payload), content_type="application/json")

    assert res.status_code == 200
    assert res.mimetype.startswith("text/plain")

    body = res.get_data(as_text=True).strip()
    assert body != ""

def test_merge_bad_request_route(client):
    bad_payload = {
        # memos 누락
        "end_flag": False,
        "advanced_flag": False,
        "temperature": 0.8,
        "style_prompt": {},
        "style_examples": [],
        "style_vector": [],
    }

    res = client.post("/merge/", data=json.dumps(bad_payload), content_type="application/json")

    assert res.status_code == 400
    assert "error" in res.get_json()


def test_l2norm_basic_and_zero():
    vec = np.array([[3.0, 4.0]], dtype=np.float32)
    out = l2norm(vec)
    assert np.allclose(out, np.array([[0.6, 0.8]], dtype=np.float32), atol=1e-5)

    zero = np.zeros((1, 3), dtype=np.float32)
    out_zero = l2norm(zero)
    assert np.all(out_zero == 0.0)


def test_count_sentences_various():
    assert count_sentences("") == 0
    assert count_sentences("한 문장입니다.") == 1
    assert count_sentences("첫 문장입니다. 두 번째 문장입니다!") == 2
    text = "피곤했다.\n그래도 공부했다?\n잘해냈다!"
    assert count_sentences(text) == 3


def test_choose_best_sentence_empty_candidates():
    assert choose_best_sentence([], np.array([1.0, 0.0])) is None


def test_choose_best_sentence_with_mocked_embeddings(monkeypatch):
    from ai.services.merge import merge_service as ms

    def fake_embed(sentences):
        return np.array([[1.0, 0.0], [0.5, 0.0], [0.1, 0.0]][:len(sentences)])

    monkeypatch.setattr(ms, "embed_sentences", fake_embed)

    candidates = ["첫 번째", "두 번째", "세 번째"]
    best = choose_best_sentence(candidates, np.array([1.0, 0.0]))
    assert best == "첫 번째"


def test_embed_sentences_shape_and_norm():
    sentences = ["테스트 문장", "두 번째 문장"]
    E = embed_sentences(sentences)

    assert isinstance(E, np.ndarray)
    assert E.shape[0] == len(sentences)
    assert np.allclose(np.linalg.norm(E, axis=-1), 1.0, atol=1e-3)



def test_merge_rerank_full_flow(monkeypatch):
    from ai.services.merge import merge_service as ms

    def fake_embed(sentences):
        return np.array([[1.0, 0.0] for _ in sentences])

    monkeypatch.setattr(ms, "embed_sentences", fake_embed)

    # OpenAI mock (세 번 호출)
    class FakeMessage:
        def __init__(self, content):
            self.content = content

    class FakeChoice:
        def __init__(self, content):
            self.message = FakeMessage(content)

    class FakeResponse:
        def __init__(self, content):
            self.choices = [FakeChoice(content)]

    call_state = {"count": 0}

    def fake_create(*a, **kw):
        call_state["count"] += 1
        if call_state["count"] == 1:
            return FakeResponse("첫번째 단락입니다.\n###\n두번째 단락입니다.")
        elif call_state["count"] == 2:
            return FakeResponse("")  # blocks empty → skip
        else:
            return FakeResponse("첫번째 단락입니다.\n###\n다른 단락입니다.")  # duplicate skip

    monkeypatch.setattr(ms.client.chat.completions, "create", fake_create)

    diary = merge_rerank(
        memos=["첫 메모", "두 번째", "세 번째"],
        style_prompt={"dummy": "x"},
        style_examples=["예시"],
        style_vector=[1.0, 0.0],
        num_candidates=2,
    )

    assert diary.count("첫번째 단락입니다.") == 1
    assert isinstance(diary, str)
    assert diary.strip() != ""
