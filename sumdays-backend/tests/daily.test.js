const dailyController = require('../controllers/dailyController');
const db = require('../db/db');

// DB를 Mock 처리
jest.mock('../db/db', () => ({
  query: jest.fn(),
}));

// fs, path, process도 Mock (실제 파일 접근 방지)
jest.mock('fs');
jest.mock('path');
jest.mock('process', () => ({
  cwd: jest.fn(() => '/mock/cwd'),
}));

const mockRes = () => {
  const res = {};
  res.status = jest.fn().mockReturnValue(res);
  res.json = jest.fn().mockReturnValue(res);
  return res;
};

describe('dailyController', () => {
  let req, res;

  beforeEach(() => {
    jest.clearAllMocks();
    res = mockRes();
    req = { user: { userId: 1 }, params: {}, body: {}, query: {} };
  });

  /* -------------------------------------------------------------------------- */
  /* GET /api/db/daily/:date                                                    */
  /* -------------------------------------------------------------------------- */
  test('getDailyEntry: should return 404 if no entry found', async () => {
    req.params.date = '2025-11-02';
    db.query.mockResolvedValueOnce([[]]); // no entry

    await dailyController.getDailyEntry(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry not found' });
  });

  test('getDailyEntry: should return entry with memos and photos', async () => {
    req.params.date = '2025-11-02';

    // 1. daily_entries
    db.query
      .mockResolvedValueOnce([[{ id: 1, user_id: 1, entry_date: '2025-11-02', icon_name: 'sun', diary: 'good day', ai_comment: 'nice' }]])
      // 2. memos
      .mockResolvedValueOnce([[{ inner_id: 1, memo_order: 1, content: 'memo', memo_time: '09:00' }]])
      // 3. photos
      .mockResolvedValueOnce([[{ inner_id: 1, photo_order: 1, extension: 'jpg' }]]);

    const fs = require('fs');
    const path = require('path');
    fs.existsSync.mockReturnValue(true);
    fs.readFileSync.mockReturnValue(Buffer.from('fakeimage'));
    path.join.mockReturnValue('/mock/path/file.jpg');

    await dailyController.getDailyEntry(req, res);

    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith(expect.objectContaining({
      date: '2025-11-02',
      memos: expect.any(Array),
      photos: expect.any(Array)
    }));
  });

  /* -------------------------------------------------------------------------- */
  /* POST /api/db/daily                                                         */
  /* -------------------------------------------------------------------------- */
  test('createDailyEntry: should return 409 if entry exists', async () => {
    req.body.date = '2025-11-02';
    db.query.mockResolvedValueOnce([[{ id: 1 }]]);

    await dailyController.createDailyEntry(req, res);
    expect(res.status).toHaveBeenCalledWith(409);
    expect(res.json).toHaveBeenCalledWith({ message: 'Entry already exists for this date' });
  });

  test('createDailyEntry: should create new entry', async () => {
    req.body.date = '2025-11-02';
    db.query
      .mockResolvedValueOnce([[]]) // no existing entry
      .mockResolvedValueOnce([]);  // insert OK

    await dailyController.createDailyEntry(req, res);
    expect(res.status).toHaveBeenCalledWith(201);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry created successfully' });
  });

  /* -------------------------------------------------------------------------- */
  /* PATCH /api/db/daily/:date                                                  */
  /* -------------------------------------------------------------------------- */
  test('updateDailyEntry: should return 400 if no fields provided', async () => {
    req.params.date = '2025-11-02';
    db.query.mockResolvedValueOnce([[{ id: 1 }]]);

    await dailyController.updateDailyEntry(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
    expect(res.json).toHaveBeenCalledWith({ message: 'No valid fields provided for update' });
  });

  test('updateDailyEntry: should update diary field', async () => {
    req.params.date = '2025-11-02';
    req.body.diary = 'updated diary';
    db.query.mockResolvedValueOnce([[{ id: 1 }]]);
    db.query.mockResolvedValueOnce([]); // update ok

    await dailyController.updateDailyEntry(req, res);
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry updated successfully' });
  });

  /* -------------------------------------------------------------------------- */
  /* DELETE /api/db/daily/:date                                                 */
  /* -------------------------------------------------------------------------- */
  test('deleteDailyEntry: should delete entry', async () => {
    req.params.date = '2025-11-02';
    db.query.mockResolvedValueOnce([]);

    await dailyController.deleteDailyEntry(req, res);
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry deleted successfully' });
  });
});
