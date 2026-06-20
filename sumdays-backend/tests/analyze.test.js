const request = require("supertest");
const axios = require("axios");
const app = require("../app");

jest.mock("axios");

// 공용 더미 데이터
const dummyDiaries = {
  diaries: [
    {
      date: "2025-10-13",
      diary: "오늘부터 본격적으로 시험 공부를 시작했다.",
      emoji: "📚",
      emotion_score: 0.2,
    },
    {
      date: "2025-10-14",
      diary: "도서관에서 하루 종일 공부했다.",
      emoji: "☕",
      emotion_score: 0.5,
    },
    {
      date: "2025-10-15",
      diary: "어제 외운 내용이 기억이 잘 나지 않아 속상했다.",
      emoji: "😣",
      emotion_score: -0.4,
    },
    {
      date: "2025-10-16",
      diary: "스터디 친구들과 문제를 풀었다.",
      emoji: "🧠",
      emotion_score: 0.6,
    },
    {
      date: "2025-10-17",
      diary: "집중이 안 돼 SNS만 봤다.",
      emoji: "😔",
      emotion_score: -0.5,
    },
    {
      date: "2025-10-18",
      diary: "시험이 코앞이라 긴장됐다.",
      emoji: "✏️",
      emotion_score: 0.3,
    },
    {
      date: "2025-10-19",
      diary: "시험이 끝나 해방감이 들었다!",
      emoji: "🎉",
      emotion_score: 0.9,
    },
  ],
};

describe("Analyze + Weekly + Monthly Summary", () => {
  test("analyze → 200", async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        ai_comment: "친구들과의 대화로 기분이 좋아진 하루였습니다.",
        analysis: { emotion_score: 0.7, keywords: ["친구들"] },
        diary: "오늘은 친구들과 카페에 가서 이야기를 많이 나눴다.",
        entry_date: null,
        icon: "😊",
        user_id: null,
      },
    });

    const res = await request(app)
      .post("/api/ai/analyze")
      .send({ diary: "오늘은 친구들과..." });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

  test("summarize-week → diaries 배열 → 200", async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        emotion_analysis: { emotion_score: 0, trend: "increasing" },
        highlights: [],
        insights: {},
        summary: {},
      },
    });

    const res = await request(app)
      .post("/api/ai/summarize-week")
      .send(dummyDiaries);

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });
});

/* ===== 에러 테스트 ===== */

test("analyze → diary 없음 → 400", async () => {
  const res = await request(app)
    .post("/api/ai/analyze")
    .send({});
  expect(res.status).toBe(400);
  expect(res.body.success).toBe(false);
});

test("analyze → Flask 실패 → 500", async () => {
  axios.post.mockRejectedValueOnce(new Error("flask died"));
  const res = await request(app)
    .post("/api/ai/analyze")
    .send({ diary: "text" });
  expect(res.status).toBe(500);
  expect(res.body.success).toBe(false);
});

// summarize-week은 이제 diaries 필수
test("summarize-week → diaries 없음 → 404", async () => {
  const res = await request(app)
    .post("/api/ai/summarize-week")
    .send({}); // diaries 없음

  expect(res.status).toBe(404);
});

