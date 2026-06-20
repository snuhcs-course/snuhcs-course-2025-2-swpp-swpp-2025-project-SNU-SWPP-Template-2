const request = require("supertest");
const path = require("path");
const axios = require("axios");
const app = require("../app");

// axios를 컨트롤러에서 사용하는 형태 그대로 mock 함
jest.mock("axios");

describe("OCR API Tests", () => {

  describe("POST /api/ai/ocr/memo", () => {

    test("✅ 이미지 + type=extract → 성공 응답", async () => {
      axios.post.mockResolvedValueOnce({
        data: { type: "extract", text: "mocked extracted text" }
      });

      const res = await request(app)
        .post("/api/ai/ocr/memo")
        .field("type", "extract") // form-data text field
        .attach("image", path.join(__dirname, "../testcase/ocr_test_한국어.jpg")); 

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.type).toBe("extract");
      expect(res.body.result).toBe("mocked extracted text");
    });

    test("❗ 파일 없이 요청 → 400 반환", async () => {
      const res = await request(app)
        .post("/api/ai/ocr/memo")
        .field("type", "extract");

      expect(res.status).toBe(400);
      expect(res.body).toHaveProperty("error", "No image file uploaded.");
    });

    test("❗ 잘못된 type 값 → 400 반환", async () => {
      const res = await request(app)
        .post("/api/ai/ocr/memo")
        .field("type", "wrongType")
        .attach("image", path.join(__dirname, "../testcase/ocr_test_한국어.jpg"));

      expect(res.status).toBe(400);
      expect(res.body.error).toBe("Invalid analysis type.");
    });

    test("❗ Flask 서버 실패 → 500 반환", async () => {
      axios.post.mockRejectedValueOnce(new Error("Flask Error"));

      const res = await request(app)
        .post("/api/ai/ocr/memo")
        .field("type", "extract")
        .attach("image", path.join(__dirname, "../testcase/ocr_test_한국어.jpg"));

      expect(res.status).toBe(500);
      expect(res.body).toHaveProperty("error");
    });

  });


  describe("POST /api/ai/ocr/diary", () => {

    test("✅ 여러 이미지 업로드 성공", async () => {
      axios.post.mockResolvedValueOnce({
        data: { result: ["page1 text", "page2 text"] }
      });

      const res = await request(app)
        .post("/api/ai/ocr/diary")
        .attach("image", path.join(__dirname, "../testcase/일기 예시.jpg"))
        .attach("image", path.join(__dirname, "../testcase/일기 예시2.jpg"));

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.result.length).toBe(2);
    });

    test("❗ 파일 없이 diary 호출 → 400", async () => {
      const res = await request(app).post("/api/ai/ocr/diary");
      expect(res.status).toBe(400);
      expect(res.body.error).toBe("No image files uploaded.");
    });

  });

});
