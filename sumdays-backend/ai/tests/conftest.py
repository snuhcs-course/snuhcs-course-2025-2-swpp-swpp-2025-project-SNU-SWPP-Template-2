import os
from dotenv import load_dotenv
load_dotenv(dotenv_path=os.path.join(os.path.dirname(__file__), "..", "..", ".env"))

import sys
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "../..")))

import pytest
from ai.app import app



@pytest.fixture(scope="session", autouse=True)
def load_env():
    env_path = os.path.join(os.path.dirname(__file__), "..", "..", ".env")
    if os.path.exists(env_path):
        load_dotenv(dotenv_path=env_path)
    else:
        print("[WARN] .env 파일을 찾을 수 없습니다:", env_path)
    os.environ.setdefault("AI_SERVER_PORT", "5001")


@pytest.fixture
def client():
    app.config["TESTING"] = True
    app.config["WTF_CSRF_ENABLED"] = False
    return app.test_client()
