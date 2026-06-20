const request = require("supertest");
const axios = require("axios");
const app = require("../app");

jest.mock("axios");

describe("Merge API Tests (/api/ai/merge)", () => {

  const endpoint = "/api/ai/merge";

  //
  // 1) memos.length < 2 → 400
  //
  test("❗ memos.length < 2 → 400", async () => {
    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [{ id: 1, content: "하나", order: 1 }],
        style_prompt: {},
        style_examples: [],
        end_flag: false
      });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/At least two memos/);
  });

  //
  // 2) style_prompt/style_examples 누락 → 400
  //
  test("❗ style_prompt, style_examples 누락 → 400", async () => {
    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, order: 1, content: "아침" },
          { id: 2, order: 2, content: "점심" }
        ],
        end_flag: false
      });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
    expect(res.body.message).toMatch(/style_prompt and style_examples/);
  });


  //
  // 3) end_flag=false → streaming pipe 테스트
  //
  test("✅ end_flag=false → streaming 처리 성공", async () => {
    // supertest로 streaming을 직접 받을 수 있도록 mock stream 생성
    const { Readable } = require("stream");
    const stream = new Readable({
      read() {
        this.push("STREAMED_RESULT");
        this.push(null);
      }
    });

    axios.post.mockResolvedValueOnce({
      data: stream
    });

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "아침", order: 1 },
          { id: 2, content: "저녁", order: 2 }
        ],
        style_prompt: {},
        style_examples: [],
        end_flag: false
      });

    // 스트리밍이면 JSON 응답이 아님.
    expect(res.status).toBe(200); 
    expect(res.headers["content-type"]).toContain("text/plain");
    expect(res.text).toBe("STREAMED_RESULT");
  });


  //
  // 4) end_flag=false → Flask에서 이상한 stream 반환 → 500
  //
  test("❗ end_flag=false + invalid stream → 500", async () => {
    axios.post.mockResolvedValueOnce({
      data: null
    });

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "아침", order: 1 },
          { id: 2, content: "저녁", order: 2 }
        ],
        style_prompt: {},
        style_examples: [],
        end_flag: false
      });

    // mergeController는 내부 오류를 catch → 500
    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
  });


  //
  // 5) end_flag=true 정상 응답
  //
  test("✅ end_flag=true + 정상 응답 → 200 + result", async () => {
    axios.post.mockResolvedValueOnce({
      data: { final_text: "하루 일기 완성!" }
    });

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "아침", order: 1 },
          { id: 2, content: "점심", order: 2 },
          { id: 3, content: "저녁", order: 3 }
        ],
        style_prompt: { tone: "밝음" },
        style_examples: ["예시 문장"],
        end_flag: true
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.result).toHaveProperty("final_text");
  });


  //
  // 6) end_flag=true + invalid response → 500
  //
  test("❗ end_flag=true + invalid response → 500", async () => {
    axios.post.mockResolvedValueOnce({ data: null });

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "아침", order: 1 },
          { id: 2, content: "저녁", order: 2 }
        ],
        style_prompt: {},
        style_examples: [],
        end_flag: true
      });

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
  });


  //
  // 7) Flask axios 호출 자체 실패 → 500
  //
  test("❗ Flask 호출 실패 → 500", async () => {
    axios.post.mockRejectedValueOnce(new Error("Flask fail"));

    const res = await request(app)
      .post(endpoint)
      .send({
        memos: [
          { id: 1, content: "내용1", order: 1 },
          { id: 2, content: "내용2", order: 2 }
        ],
        style_prompt: {},
        style_examples: [],
        end_flag: false
      });

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false);
  });

});
