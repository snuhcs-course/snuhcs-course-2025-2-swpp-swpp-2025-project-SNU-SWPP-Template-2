import pytest
import json
from ai.app import app

@pytest.fixture
def client():
    app.config["TESTING"] = True
    return app.test_client()

def test_analyze_diary_integration(client):
    payload = {
        "diary": "오늘은 친구와 카페에서 이야기를 나눴다. 행복한 하루였다."
    }
    response = client.post("/analysis/diary", data=json.dumps(payload),
                           content_type="application/json")
    assert response.status_code == 200
    data = response.get_json()
    
    assert "analysis" in data
    assert "icon" in data
    assert "ai_comment" in data
    assert isinstance(data["analysis"], dict)
    assert "keywords" in data["analysis"]
    assert "emotion_score" in data["analysis"]

def test_analysis_diary_missing_field(client):
    res = client.post("/analysis/diary", data=json.dumps({}),
                      content_type="application/json")
    data = res.get_json()

    assert res.status_code == 400
    assert "error" in data