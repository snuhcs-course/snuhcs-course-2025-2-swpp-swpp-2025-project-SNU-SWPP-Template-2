const request = require("supertest");
const app = require("../../app");

describe("E2E Merge API (Node → Flask → OpenAI)", () => {

  // 공통 dummy style_prompt/style_examples
  const STYLE_PROMPT = { tone: "casual" };
  const STYLE_EXAMPLES = ["예시 문장입니다."];

  test("flag=false → streaming merge 결과 검증 (문자열 스트림)", async () => {
    const res = await request(app)
      .post("/api/ai/merge")
      .set("Content-Type", "application/json")
      .send({
        memos: [
          { content: "아침을 굶었다.", order: 1 },
          { content: "점심도 굶었다.", order: 2 },
          { content: "저녁도 굶을거다.", order: 3 }
        ],
        style_prompt: STYLE_PROMPT,
        style_examples: STYLE_EXAMPLES,
        style_vector: [0.1,0.2],
        end_flag: false,
        temperature: 0.8,
        advanced_flag: false
      });

    // 스트리밍은 JSON이 아님 → res.body 없음 → 대신 res.text 존재
    expect(res.status).toBe(200);
    expect(res.headers["content-type"]).toContain("text/plain");

    // 스트림 결과는 문자열이어야 한다
    expect(typeof res.text).toBe("string");
    expect(res.text.length).toBeGreaterThan(0);
  });

  test("flag=true → 완료된 diary JSON 구조 검증", async () => {
    const res = await request(app)
      .post("/api/ai/merge")
      .set("Content-Type", "application/json")
      .send({
        memos: [
          { content: "아침", order: 1 },
          { content: "점심", order: 2 },
          { content: "저녁", order: 3 }
        ],
        style_prompt: STYLE_PROMPT,
        style_examples: STYLE_EXAMPLES,
        style_vector: [0.1,0.2],
        end_flag: true,
        temperature: 0.8,
        advanced_flag: false
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);

    // Flask merge()가 return하는 구조에 맞춰 확인
    expect(res.body.result).toHaveProperty("diary");
    expect(res.body.result).toHaveProperty("analysis");
    expect(res.body.result).toHaveProperty("ai_comment");

    expect(typeof res.body.result.diary).toBe("string");
    expect(res.body.result.diary.length).toBeGreaterThan(0);
  });

});
