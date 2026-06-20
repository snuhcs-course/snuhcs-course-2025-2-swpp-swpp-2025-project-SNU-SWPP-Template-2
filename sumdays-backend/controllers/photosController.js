const db = require('../db/db');
const fs = require("fs");
const path = require("path");

/* -------------------------------------------------------------------------- */
/*  1️⃣ POST /api/db/photos/:date/photos — 사진 생성                            */
/* -------------------------------------------------------------------------- */

async function getDailyEntryId(userId, date) {
  const [rows] = await db.query(
    'SELECT id FROM daily_entry WHERE user_id = ? AND entry_date = ?',
    [userId, date]
  );
  return rows.length > 0 ? rows[0].id : null;
}

exports.createPhoto = async (req, res) => {
  const { date } = req.params;
  const { base64Image, inner_id, photo_order } = req.body;
  const userId = req.user.userId

  try {
    // 1. store server - data 
    const uploadDir = path.join(process.cwd(), "uploads", userId.toString(), date);
    if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir, { recursive: true });

    const matches = base64Image.match(/^data:(.+);base64,(.+)$/);
    if (!matches) return res.status(400).json({ error: "Invalid base64 format" });

    const mimeType = matches[1];
    const extension = mimeType.split("/")[1];
    const imageData = matches[2];
    const filePath = path.join(uploadDir, `${inner_id}.${extension}`);
    fs.writeFileSync(filePath, Buffer.from(imageData, "base64"));


    // 2. store db - path 
    const daily_entry_id = await getDailyEntryId(userId, date)
    if (daily_entry_id === null) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }
    await db.query(
      'INSERT INTO photos (daily_entry_id, inner_id, photo_order, extension) VALUES (?, ?, ?, ?)',
      [daily_entry_id, inner_id, photo_order, extension]
    );

    res.status(201).json({ message: 'Photo created successfully' });
  } catch (error) {
    // console.error('❌ [createPhoto] Error:', error);
    res.status(500).json({ error: 'Database insert failed' });
  }
};


/* -------------------------------------------------------------------------- */
/*  2️⃣ DELETE /api/db/photos/:date/photos/:inner_id — 사진 삭제                */
/* -------------------------------------------------------------------------- */
exports.deletePhoto = async (req, res) => {
  const {date, inner_id } = req.params;
  const userId = req.user.userId

  try {
    const daily_entry_id = await getDailyEntryId(userId, date)
    if (daily_entry_id === null) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }
    // 1. db delete
    await db.query('DELETE FROM photos WHERE daily_entry_id = ? AND inner_id = ?', [daily_entry_id, inner_id]);
    // 2. server delete 
    const filePath = path.join(process.cwd(), "uploads", userId.toString(), date, `${inner_id}.jpg`);
    if (fs.existsSync(filePath)) fs.unlinkSync(filePath);
    res.status(200).json({ message: 'Photo deleted successfully' });

  } catch (error) {
    // console.error('❌ [deletePhoto] Error:', error);
    res.status(500).json({ error: 'Database delete failed' });
  }
};


/* -------------------------------------------------------------------------- */
/*  3️⃣ PATCH /api/db/photos/:date/photos/order — 사진 순서 변경                */
/* -------------------------------------------------------------------------- */
exports.reorderPhotos = async (req, res) => {
  const { date } = req.params;
  const orderMap = req.body; // { "1": 2, "2": 1, "3": 3 }
  const userId = req.user.userId 

  try {
    const daily_entry_id = await getDailyEntryId(userId, date)
    if (daily_entry_id === null) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

    const updatePromises = Object.entries(orderMap).map(([inner_id, newOrder]) =>
      db.query('UPDATE photos SET photo_order = ? WHERE daily_entry_id = ? AND inner_id = ?', [newOrder, daily_entry_id, inner_id])
    );

    await Promise.all(updatePromises);
    res.status(200).json({ message: 'Photo order updated successfully' });
  } catch (error) {
    // console.error('❌ [reorderPhotos] Error:', error);
    res.status(500).json({ error: 'Database update failed' });
  }
};
