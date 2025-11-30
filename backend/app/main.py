from fastapi import FastAPI

from .features.auth.router import router as auth_router
from .features.journal.router import router as journal_router
from .features.statistics.router import router as statistics_router
from .features.user.router import router as user_router
from .features.selfaware.router import router as self_aware_router

app = FastAPI(title="MindLog")

app.include_router(auth_router, prefix="/api/v1")
app.include_router(user_router, prefix="/api/v1")
app.include_router(journal_router, prefix="/api/v1")
app.include_router(statistics_router, prefix="/api/v1")
app.include_router(self_aware_router, prefix="/api/v1")

@app.get("/")
def read_root():
    return {"message": "Hello World!"}
