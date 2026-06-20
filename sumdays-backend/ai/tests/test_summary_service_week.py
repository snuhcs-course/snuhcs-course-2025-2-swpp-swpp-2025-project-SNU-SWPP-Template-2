import pytest
import json
from ai.app import app

@pytest.fixture
def client():
    app.config["TESTING"] = True
    return app.test_client()

def test_summarize_week_integration(client):
    diaries = [
        {"text": "오늘은 즐거운 하루였다."},
        {"text": "조금 피곤했지만 괜찮았다."},
        {"text": "스트레스가 많았다."}
    ]
    response = client.post("/analysis/week", 
                           data=json.dumps({"diaries": diaries}),
                           content_type="application/json")
    assert response.status_code == 200
    data = response.get_json()

    assert "summary" in data
    assert "emotion_analysis" in data
    assert "highlights" in data
    assert "insights" in data

    summary = data["summary"]
    assert "title" in summary
    assert "overview" in summary
    assert "emerging_topics" in summary

def test_analysis_week_invalid_body(client):
    res = client.post("/analysis/week", data=json.dumps({"wrong": []}),
                      content_type="application/json")
    data = res.get_json()

    assert res.status_code == 400
    assert "error" in data