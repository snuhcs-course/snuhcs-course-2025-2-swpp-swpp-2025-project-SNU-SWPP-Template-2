const photosController = require('../controllers/photosController');
const db = require('../db/db');

// Mock modules
jest.mock('../db/db', () => ({
  query: jest.fn(),
}));
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

describe('photosController', () => {
  let req, res;
  const fs = require('fs');
  const path = require('path');

  beforeEach(() => {
    jest.clearAllMocks();
    res = mockRes();
    req = { user: { userId: 1 }, params: {}, body: {} };
  });

  /* -------------------------------------------------------------------------- */
  /* createPhoto                                                                */
  /* -------------------------------------------------------------------------- */
  test('createPhoto: should return 400 if base64 format invalid', async () => {
    req.params.date = '2025-11-02';
    req.body = { base64Image: 'invalidbase64', inner_id: '1', photo_order: 1 };

    await photosController.createPhoto(req, res);
    expect(res.status).toHaveBeenCalledWith(400);
    expect(res.json).toHaveBeenCalledWith({ error: 'Invalid base64 format' });
  });

  test('createPhoto: should return 404 if daily entry not found', async () => {
    req.params.date = '2025-11-02';
    req.body = {
      base64Image: 'data:image/jpeg;base64,aGVsbG8=',
      inner_id: '1',
      photo_order: 1,
    };

    fs.existsSync.mockReturnValue(true);
    fs.writeFileSync.mockReturnValue(undefined);
    path.join.mockReturnValue('/mock/path/1.jpg');
    db.query.mockResolvedValueOnce([[]]); // getDailyEntryId → not found

    await photosController.createPhoto(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry not found' });
  });

  test('createPhoto: should insert photo successfully', async () => {
    req.params.date = '2025-11-02';
    req.body = {
      base64Image: 'data:image/jpeg;base64,aGVsbG8=',
      inner_id: '1',
      photo_order: 1,
    };

    fs.existsSync.mockReturnValue(false);
    fs.mkdirSync.mockReturnValue(undefined);
    fs.writeFileSync.mockReturnValue(undefined);
    path.join.mockReturnValue('/mock/path/1.jpg');

    db.query
      .mockResolvedValueOnce([[{ id: 10 }]]) // getDailyEntryId ok
      .mockResolvedValueOnce([]); // insert ok

    await photosController.createPhoto(req, res);
    expect(res.status).toHaveBeenCalledWith(201);
    expect(res.json).toHaveBeenCalledWith({ message: 'Photo created successfully' });
  });

  /* -------------------------------------------------------------------------- */
  /* deletePhoto                                                                */
  /* -------------------------------------------------------------------------- */
  test('deletePhoto: should return 404 if no daily entry found', async () => {
    req.params = { date: '2025-11-02', inner_id: '1' };
    db.query.mockResolvedValueOnce([[]]);

    await photosController.deletePhoto(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry not found' });
  });

  test('deletePhoto: should delete photo and file successfully', async () => {
    req.params = { date: '2025-11-02', inner_id: '1' };
    fs.existsSync.mockReturnValue(true);
    fs.unlinkSync.mockReturnValue(undefined);
    path.join.mockReturnValue('/mock/path/1.jpg');
    db.query
      .mockResolvedValueOnce([[{ id: 10 }]]) // getDailyEntryId ok
      .mockResolvedValueOnce([]); // delete ok

    await photosController.deletePhoto(req, res);
    expect(fs.unlinkSync).toHaveBeenCalled();
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({ message: 'Photo deleted successfully' });
  });

  /* -------------------------------------------------------------------------- */
  /* reorderPhotos                                                              */
  /* -------------------------------------------------------------------------- */
  test('reorderPhotos: should return 404 if daily entry not found', async () => {
    req.params.date = '2025-11-02';
    req.body = { '1': 2, '2': 1 };
    db.query.mockResolvedValueOnce([[]]); // getDailyEntryId fail

    await photosController.reorderPhotos(req, res);
    expect(res.status).toHaveBeenCalledWith(404);
    expect(res.json).toHaveBeenCalledWith({ message: 'Daily entry not found' });
  });

  test('reorderPhotos: should reorder successfully', async () => {
    req.params.date = '2025-11-02';
    req.body = { '1': 2, '2': 1 };
    db.query.mockResolvedValueOnce([[{ id: 10 }]]); // getDailyEntryId ok
    db.query.mockResolvedValue([]); // each update ok

    await photosController.reorderPhotos(req, res);
    expect(res.status).toHaveBeenCalledWith(200);
    expect(res.json).toHaveBeenCalledWith({ message: 'Photo order updated successfully' });
  });
});
