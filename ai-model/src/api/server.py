"""FastAPI server exposing the hybrid RF + LLM counter-proposal pipeline."""

from __future__ import annotations

from pathlib import Path
from typing import List, Sequence

from fastapi import FastAPI
from pydantic import BaseModel, Field
from dotenv import load_dotenv

from models.random_forest import BookFeature
from pipeline.hybrid_exchange import (
    CounterProposal,
    ExchangeScenario,
    HybridExchangePipeline,
    ParticipantProfile,
    ReasonedProposal,
)

load_dotenv(Path(__file__).resolve().parents[2] / ".env", override=False)

app = FastAPI(title="Hybrid Exchange API", version="0.1.0")
pipeline = HybridExchangePipeline()


class BookPayload(BaseModel):
    id: str
    title: str
    genres: List[str] = Field(default_factory=list)
    moods: List[str] = Field(default_factory=list)
    popularity: float = 0.5
    condition_score: float = Field(default=0.8, ge=0.0, le=1.0)


class UserPayload(BaseModel):
    user_id: str = Field(..., alias="id")
    name: str
    preferred_genres: List[str] = Field(default_factory=list)
    preferred_moods: List[str] = Field(default_factory=list)
    reading_purposes: List[str] = Field(default_factory=list)

    def to_profile(self) -> ParticipantProfile:
        return ParticipantProfile(
            user_id=self.user_id,
            name=self.name,
            preferred_genres=self.preferred_genres,
            preferred_moods=self.preferred_moods,
            reading_purposes=self.reading_purposes,
        )


class ExchangeRequestPayload(BaseModel):
    requester: UserPayload
    responder: UserPayload
    requester_books: List[BookPayload]
    initial_message: str = ""
    context_note: str = ""
    top_k: int = Field(3, ge=1, le=5)


class RankedBookPayload(BaseModel):
    id: str
    title: str
    score: float
    genres: List[str]
    moods: List[str]


class CounterProposalResponse(BaseModel):
    ranked_books: List[RankedBookPayload]
    message: str


class ConversationTurnPayload(BaseModel):
    speaker: str
    message: str
    reasoning: str | None = None


class ReasoningPayload(BaseModel):
    recommended_books: List[str]
    conversation: List[ConversationTurnPayload]
    final_recommendation: str
    confidence_score: float


class ReasonedProposalResponse(CounterProposalResponse):
    reasoning: ReasoningPayload


def _build_scenario(payload: ExchangeRequestPayload) -> ExchangeScenario:
    requester_books: Sequence[BookFeature] = [
        BookFeature(
            book_id=book.id,
            title=book.title,
            genres=book.genres,
            moods=book.moods,
            popularity=book.popularity,
            condition_score=book.condition_score,
        )
        for book in payload.requester_books
    ]
    return ExchangeScenario(
        requester=payload.requester.to_profile(),
        responder=payload.responder.to_profile(),
        requester_books=requester_books,
        initial_message=payload.initial_message,
        context_note=payload.context_note,
    )


def _to_response(proposal: CounterProposal) -> CounterProposalResponse:
    ranked_payload = [
        RankedBookPayload(
            id=entry.book.book_id,
            title=entry.book.title,
            score=entry.score,
            genres=list(entry.book.genres),
            moods=list(entry.book.moods),
        )
        for entry in proposal.ranked_books
    ]
    return CounterProposalResponse(
        ranked_books=ranked_payload, message=proposal.message
    )


@app.post("/api/exchange/counter-proposal", response_model=CounterProposalResponse)
def generate_counter_proposal(payload: ExchangeRequestPayload):
    """Implements step (3) in the barter flow with RF + LLM."""
    scenario = _build_scenario(payload)
    proposal = pipeline.generate_counter_proposal(
        scenario, top_k=payload.top_k
    )
    return _to_response(proposal)


def _to_reasoned_response(proposal: ReasonedProposal) -> ReasonedProposalResponse:
    base = _to_response(proposal)
    conversation = [
        ConversationTurnPayload(
            speaker=turn.speaker,
            message=turn.message,
            reasoning=turn.reasoning,
        )
        for turn in proposal.reasoning.conversation
    ]
    reasoning_payload = ReasoningPayload(
        recommended_books=proposal.reasoning.recommended_books,
        conversation=conversation,
        final_recommendation=proposal.reasoning.final_recommendation,
        confidence_score=proposal.reasoning.confidence_score,
    )
    return ReasonedProposalResponse(
        ranked_books=base.ranked_books,
        message=base.message,
        reasoning=reasoning_payload,
    )


@app.post(
    "/api/exchange/reasoned-proposal",
    response_model=ReasonedProposalResponse,
)
def generate_reasoned_proposal(payload: ExchangeRequestPayload):
    scenario = _build_scenario(payload)
    proposal = pipeline.generate_reasoned_counter_proposal(
        scenario, top_k=payload.top_k
    )
    return _to_reasoned_response(proposal)


@app.get("/health")
def healthcheck():
    return {"status": "ok"}
