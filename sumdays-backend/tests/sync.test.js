jest.mock("../db/db", () => ({
  pool: {
    query: jest.fn(),
  },
}));

const { pool } = require("../db/db");
const syncController = require("../controllers/syncController");

// Mock Response
const mockResponse = () => {
  const res = {};
  res.json = jest.fn().mockReturnValue(res);
  res.status = jest.fn().mockReturnValue(res);
  return res;
};

describe("syncController Tests (90%↑ coverage)", () => {

  // =====================================================================
  // SYNC DATA TESTS
  // =====================================================================

  test("syncData - user auto-create & empty edited/deleted", async () => {
    const req = {
      body: {},
      user: { userId: 1 }
    };
    const res = mockResponse();

    pool.query
      .mockResolvedValueOnce([[]])  // user not exist
      .mockResolvedValueOnce([{ affectedRows: 1 }]); // insert user

    await syncController.syncData(req, res);

    expect(res.json).toHaveBeenCalledWith({
      status: "success",
      message: "Sync completed successfully."
    });
  });


  test("syncData - deleted: memo, dailyEntry, userStyle, weekSummary", async () => {
    const req = {
      body: {
        deleted: {
          memo: [1, 2],
          dailyEntry: ["2024-01-01"],
          userStyle: [10],
          weekSummary: ["2024-01-01"]
        }
      },
      user: { userId: 2 }
    };
    const res = mockResponse();

    // user exist
    pool.query.mockResolvedValue([[{ id: 2 }]]);

    await syncController.syncData(req, res);

    expect(pool.query).toHaveBeenCalled();
    expect(res.json).toHaveBeenCalled();
  });


  test("syncData - edited: memo + daily + week + userStyle", async () => {
    const req = {
      body: {
        edited: {
          memo: [{
            room_id: 1,
            content: "hi",
            timestamp: "2024",
            date: "2024-01-01",
            memo_order: 1,
            type: "text"
          }],
          dailyEntry: [{
            date: "2024-01-01",
            diary: "today good",
            keywords: "a;b;c",
            aiComment: "ok",
            emotionScore: 0.5,
            emotionIcon: "🙂",
            themeIcon: "🌙"
          }],
          weekSummary: [{
            startDate: "2024-01-01",
            endDate: "2024-01-07",
            diaryCount: 7,
            emotionAnalysis: { good: 3 },
            highlights: ["a"],
            insights: { msg: "ok" },
            summary: { s: 1 }
          }],
          userStyle: [{
            styleId: 3,
            styleName: "cute",
            styleVector: [1, 2],
            styleExamples: ["ex"],
            stylePrompt: { p: 1 },
            sampleDiary: "hi!"
          }]
        }
      },
      user: { userId: 3 }
    };
    const res = mockResponse();

    // user exist
    pool.query.mockResolvedValueOnce([[{ id: 3 }]]);

    await syncController.syncData(req, res);

    expect(res.json).toHaveBeenCalled();
  });


  test("syncData - internal error", async () => {
    pool.query.mockRejectedValueOnce(new Error("DB ERROR"));

    const req = {
      body: {},
      user: { userId: 1 }
    };

    const res = mockResponse();

    await syncController.syncData(req, res);

    expect(res.status).toHaveBeenCalledWith(500);
  });


  // =====================================================================
  // FETCH SERVER DATA TESTS
  // =====================================================================

  test("fetchServerData - success", async () => {
    const req = { user: { userId: 5 } };
    const res = mockResponse();

    pool.query
      .mockResolvedValueOnce([[{ id: 1, content: "m1" }]])  // memo
      .mockResolvedValueOnce([[{ id: 2, diary: "d" }]])      // daily
      .mockResolvedValueOnce([[{
        emotionAnalysis: '{"a":1}',
        highlights: '["a"]',
        insights: '{"i":1}',
        summary: '{"s":1}'
      }]]) // week_summary
      .mockResolvedValueOnce([[{
        styleVector: "[1,2]",
        styleExamples: '["ex"]',
        stylePrompt: '{"p":1}',
        sampleDiary: "sample"
      }]]); // user_style

    await syncController.fetchServerData(req, res);

    expect(res.json).toHaveBeenCalled();
  });


  test("fetchServerData - JSON parse error fallback", async () => {
    const req = { user: { userId: 5 } };
    const res = mockResponse();

    pool.query
      .mockResolvedValueOnce([[{}]])
      .mockResolvedValueOnce([[{}]])
      .mockResolvedValueOnce([[{
        emotionAnalysis: "invalid_json",
        highlights: "invalid_json",
        insights: "invalid_json",
        summary: "invalid_json",
      }]])
      .mockResolvedValueOnce([[{
        styleVector: "invalid_json",
        styleExamples: "invalid_json",
        stylePrompt: "invalid_json",
        sampleDiary: null
      }]]);

    await syncController.fetchServerData(req, res);

    expect(res.json).toHaveBeenCalled();
  });


  test("fetchServerData - server error", async () => {
    pool.query.mockRejectedValueOnce(new Error("FETCH ERR"));

    const req = { user: { userId: 10 } };
    const res = mockResponse();

    await syncController.fetchServerData(req, res);

    expect(res.status).toHaveBeenCalledWith(500);
  });

});
