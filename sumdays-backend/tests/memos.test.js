const memosController = require('../controllers/memosController');
const db = require('../db/db');

// DB Mock
jest.mock('../db/db', () => ({
  query: jest.fn(),
}));

const mockRes = () => {
  const res = {};
  res.status = jest.fn().mockReturnValue(res);
  res.json = jest.fn().mockReturnValue(res);
  return res;
};

describe('memosController', () => {
  let req, res;

  beforeEach(() => {
    jest.clearAllMocks();
    res = mockRes();
    req = { user: { userId: 1 }, params: {}, body: {} };
  });

  /* -------------------------------------------------------------------------- */
  /* createMemo                                                                 */
  /* -------------------------------------------------------------------------- */
  test('createMemo: should return 404 if no daily entry found', async () => {
    req.params.date = '2025-11-02';
    db.query.mockResolvedValueOnce([[]]); // getDailyEntryId → not found

    await memosController.createMemo(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry not found' });
  });

  test('createMemo: should insert memo successfully', async () => {
    req.params.date = '2025-11-02';
    req.body = { inner_id: 1, memo_order: 1, content: 'memo', memo_time: '09:00' };

    db.query
      .mockResolvedValueOnce([[{ id: 10 }]]) // getDailyEntryId → found
      .mockResolvedValueOnce([]); // insert ok

    await memosController.createMemo(req, res);
    expect(db.query).toHaveBeenCalledTimes(2);
    expect(res.status).toHaveBeenCalledWith(201);
    expect(res.json).toHaveBeenCalledWith({ message: 'memo created successfully' });
  });

  /* -------------------------------------------------------------------------- */
  /* updateMemo                                                                 */
  /* -------------------------------------------------------------------------- */
  test('updateMemo: should return 404 if daily entry not found', async () => {
    req.params = { date: '2025-11-02', inner_id: '1' };
    db.query.mockResolvedValueOnce([[]]);

    await memosController.updateMemo(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry not found' });
  });

  test('updateMemo: should return 400 if no fields provided', async () => {
    req.params = { date: '2025-11-02', inner_id: '1' };
    db.query.mockResolvedValueOnce([[{ id: 10 }]]);

    await memosController.updateMemo(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
    expect(res.json).toHaveBeenCalledWith({ message: 'No valid fields provided for update' });
  });

  test('updateMemo: should update memo successfully', async () => {
    req.params = { date: '2025-11-02', inner_id: '1' };
    req.body = { content: 'updated', memo_time: '10:00' };
    db.query.mockResolvedValueOnce([[{ id: 10 }]]); // daily entry found
    db.query.mockResolvedValueOnce([]); // update ok

    await memosController.updateMemo(req, res);
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry updated successfully' });
  });

  /* -------------------------------------------------------------------------- */
  /* deleteMemo                                                                 */
  /* -------------------------------------------------------------------------- */
  test('deleteMemo: should return 404 if daily entry not found', async () => {
    req.params = { date: '2025-11-02', inner_id: '1' };
    db.query.mockResolvedValueOnce([[]]);

    await memosController.deleteMemo(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry not found' });
  });

  test('deleteMemo: should delete memo successfully', async () => {
    req.params = { date: '2025-11-02', inner_id: '1' };
    db.query.mockResolvedValueOnce([[{ id: 10 }]]); // getDailyEntryId → ok
    db.query.mockResolvedValueOnce([]); // delete ok

    await memosController.deleteMemo(req, res);
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({ message: 'Memo deleted successfully' });
  });

  /* -------------------------------------------------------------------------- */
  /* reorderMemos                                                               */
  /* -------------------------------------------------------------------------- */
  test('reorderMemos: should return 404 if daily entry not found', async () => {
    req.params.date = '2025-11-02';
    req.body = { '1': 2 };
    db.query.mockResolvedValueOnce([[]]);

    await memosController.reorderMemos(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry not found' });
  });

  test('reorderMemos: should update memo order successfully', async () => {
    req.params.date = '2025-11-02';
    req.body = { '1': 2, '2': 1 };
    db.query.mockResolvedValueOnce([[{ id: 10 }]]); // getDailyEntryId ok
    db.query.mockResolvedValue([]); // each update ok

    await memosController.reorderMemos(req, res);
    expect(db.query).toHaveBeenCalledTimes(1 + 2); // one for getDailyEntryId + two updates
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({ message: 'memo order updated successfully' });
  });
});
