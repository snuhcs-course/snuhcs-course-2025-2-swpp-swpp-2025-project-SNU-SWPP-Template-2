const request = require("supertest");
const path = require("path");
const axios = require("axios");
const app = require("../app");

jest.mock("axios");

describe("STT API Tests", () => {

  test("✅ 오디오 업로드 + 정상 변환 → 200", async () => {
    axios.post.mockResolvedValueOnce({
      data: { transcribed_text: "mocked speech to text result" }
    });

    const res = await request(app)
      .post("/api/ai/stt/memo")
      .attach("audio", path.join(__dirname, "../testcase/sample_audio.wav")); // 파일 필요

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.transcribed_text).toBe("mocked speech to text result");
  });

  test("❗ 파일 없이 요청 → 400", async () => {
    const res = await request(app)
      .post("/api/ai/stt/memo");

    expect(res.status).toBe(400);
    expect(res.body.error).toBe("No audio file uploaded.");
  });

  test("❗ Flask 서버 실패 → 500", async () => {
    axios.post.mockRejectedValueOnce(new Error("STT failed"));

    const res = await request(app)
      .post("/api/ai/stt/memo")
      .attach("audio", path.join(__dirname, "../testcase/sample_audio.wav"));

    expect(res.status).toBe(500);
    expect(res.body.success).toBe(false); // ✅ 네 컨트롤러는 failure 시 success:false
    expect(res.body).toHaveProperty("error");
  });

});
