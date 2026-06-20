const request = require("supertest");
const path = require("path");
const axios = require("axios");
const fs = require("fs");
const app = require("../app");

jest.mock("axios");

describe("Extract Style API Tests (Node → Flask)", () => {

  beforeEach(() => {
    jest.restoreAllMocks();
    jest.clearAllMocks();
  });

  test("✅ diaries + (optional) images → 정상 200", async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        style_vector: [0.1, 0.2],
        style_examples: ["예시1"],
        style_prompt: { tone: "bright" }
      }
    });

    const res = await request(app)
      .post("/api/ai/extract-style")
      .field("diaries", JSON.stringify(["일기1", "일기2", "일기3", "일기4", "일기5"]));

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.style_vector).toBeInstanceOf(Array);
    expect(res.body.style_examples).toBeInstanceOf(Array);
    expect(res.body.style_prompt).toBeInstanceOf(Object);
  });

  test("❗ diaries 없으면 500", async () => {
    const res = await request(app)
      .post("/api/ai/extract-style");

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
  });

  test("❗ Flask 내부 오류 → 500", async () => {
    axios.post.mockRejectedValueOnce(new Error("Flask error"));

    const res = await request(app)
      .post("/api/ai/extract-style")
      .field("diaries", JSON.stringify(["일기1", "일기2", "일기3"]));

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
    expect(res.body.error).toBeDefined();
  });

  test("❗ diary가 list가 아님 → 500", async () => {
    axios.post.mockRejectedValueOnce(new Error("Flask error"));

    const res = await request(app)
      .post("/api/ai/extract-style")
      .field("diaries", JSON.stringify({ "일기1": "일기3" }));

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
  });

  test("🔎 diaries JSON 파싱 실패 → fallback 적용", async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        style_vector: [1, 2],
        style_examples: ["a"],
        style_prompt: { tone: "ok" }
      }
    });

    const res = await request(app)
      .post("/api/ai/extract-style")
      .field("diaries", "이건_JSON_아님");

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
  });

  test("unlink 실패", async () => {
    axios.post.mockResolvedValueOnce({
      data: {
        style_vector: [9],
        style_examples: ["a"],
        style_prompt: { mode: "ok" }
      }
    });

    const req = request(app)
      .post("/api/ai/extract-style")
      .field("diaries", JSON.stringify(["일기1"]))
      .attach("images", path.join(__dirname, "../testcase/일기 예시.jpg"));

    // attach 이후 mock
    const readStreamMock = jest
      .spyOn(fs, "createReadStream")
      .mockReturnValue("STREAM");

    const unlinkMock = jest
      .spyOn(fs, "unlink")
      .mockImplementation((p, cb) => cb("unlink 실패"));

    const res = await req;

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(readStreamMock).toHaveBeenCalled();
  });

});
