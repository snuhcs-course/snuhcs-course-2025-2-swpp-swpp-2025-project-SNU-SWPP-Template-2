from books.services.publication_categories import (
    PublicationCategorizer,
    PublicationPayload,
)


class _StubLLM:
    def __init__(self, response: str) -> None:
        self.response = response
        self.calls = 0

    def generate(self, *args, **kwargs):  # pragma: no cover - exercised via categorizer
        self.calls += 1
        return self.response


def _payload(description: str = "mystery case", genres: list[str] | None = None):
    return PublicationPayload(
        identifier="test",
        title="Sample",
        description=description,
        authors=["Author"],
        genres=genres or ["Mystery"],
    )


def test_categorizer_prefers_llm_response():
    llm = _StubLLM("[{\"label\": \"Psychological Fiction\", \"score\": 0.91}]")
    categorizer = PublicationCategorizer(llm_client=llm, enable_llm=False)

    result = categorizer.classify(_payload())

    assert llm.calls == 1
    assert result.category_scores[0]["label"] == "Psychological Fiction"
    assert result.category_scores[0]["score"] == 0.91
    assert result.taste_profile["genres"], "Expected taste profile fallback"


def test_categorizer_falls_back_to_heuristics():
    categorizer = PublicationCategorizer(enable_llm=False)

    result = categorizer.classify(
        _payload(description="A hopeful coming-of-age story", genres=["Coming Of Age"])
    )

    assert result.category_scores, "Expected heuristic categories"
    top = result.category_scores[0]
    assert 0 <= top["score"] <= 1
    assert result.taste_profile["genres"]
    assert result.taste_profile["moods"]
