const db = require('../db/db');
const fs = require("fs");
const path = require("path");

/* -------------------------------------------------------------------------- */
/*  1️⃣ POST /api/db/memos/:date/memos — 메모 생성                            */
/* -------------------------------------------------------------------------- */

async function getDailyEntryId(userId, date) {
  const [rows] = await db.query(
    'SELECT id FROM daily_entry WHERE user_id = ? AND entry_date = ?',
    [userId, date]
  );
  return rows.length > 0 ? rows[0].id : null;
}

exports.createMemo = async (req, res) => {
  const { date } = req.params;
  const { inner_id, memo_order, content, memo_time } = req.body;
  const userId = req.user.userId

  try {
    const daily_entry_id = await getDailyEntryId(userId, date)
    if (daily_entry_id === null) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }
    await db.query(
      'INSERT INTO memos (daily_entry_id, inner_id, memo_order, content, memo_time) VALUES (?, ?, ?, ?, ?)',
      [daily_entry_id, inner_id, memo_order, content, memo_time]
    );

    res.status(201).json({ message: 'memo created successfully' });
  } catch (error) {
    // console.error('❌ [creatememo] Error:', error);
    res.status(500).json({ error: 'Database insert failed' });
  }
};

/* -------------------------------------------------------------------------- */
/*  2️⃣ PATCH /api/db/memos/:date/memos/:inner_id — 메모 수정             */
/* -------------------------------------------------------------------------- */
exports.updateMemo = async (req, res) => {
  const {date, inner_id } = req.params;
  const { memo_order, content, memo_time } = req.body;
  const userId = req.user.userId

  try {
    const daily_entry_id = await getDailyEntryId(userId, date)
    if (daily_entry_id === null) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

    // 1. make query ()
    const updates = [];
    const values = [];
    if (memo_order !== undefined) {
    updates.push('memo_order = ?');
    values.push(memo_order);
    }
    if (content !== undefined) {
    updates.push('content = ?');
    values.push(content);
    }
    if (memo_time !== undefined) {
    updates.push('memo_time = ?');
    values.push(memo_time);
    }
    if (updates.length === 0) {
    return res.status(400).json({ message: 'No valid fields provided for update' });
    }
    // 2. 실행 
    values.push(daily_entry_id, inner_id);
    const sql = `UPDATE memos SET ${updates.join(', ')} WHERE daily_entry_id = ? AND inner_id = ?`;
    await db.query(sql, values);

    res.status(200).json({ message: 'Daily entry updated successfully' });
    } catch (error) {
    // console.error('❌ [updatememo] Error:', error);
    res.status(500).json({ error: 'Database delete failed' });
  }
};

/* -------------------------------------------------------------------------- */
/*  2️⃣ DELETE /api/db/memos/:date/memos/:inner_id — 사진 삭제                */
/* -------------------------------------------------------------------------- */
exports.deleteMemo = async (req, res) => {
  const {date, inner_id } = req.params;
  const userId = req.user.userId

  try {
    const daily_entry_id = await getDailyEntryId(userId, date)
    if (daily_entry_id === null) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }
    // 1. db delete
    await db.query('DELETE FROM memos WHERE daily_entry_id = ? AND inner_id = ?', [daily_entry_id, inner_id]);
    res.status(200).json({ message: 'Memo deleted successfully' });
  } catch (error) {
    // console.error('❌ [deletememo] Error:', error);
    res.status(500).json({ error: 'Database delete failed' });
  }
};


/* -------------------------------------------------------------------------- */
/*  3️⃣ PATCH /api/db/memos/:date/memos/order — 사진 순서 변경                */
/* -------------------------------------------------------------------------- */
exports.reorderMemos = async (req, res) => {
  const { date } = req.params;
  const orderMap = req.body; // { "1": 2, "2": 1, "3": 3 }
  const userId = req.user.userId 

  try {
    const daily_entry_id = await getDailyEntryId(userId, date)
    if (daily_entry_id === null) {
      return res.status(404).json({ message: 'Daily entry not found' });
    }

    const updatePromises = Object.entries(orderMap).map(([inner_id, newOrder]) =>
      db.query('UPDATE memos SET memo_order = ? WHERE daily_entry_id = ? AND inner_id = ?', [newOrder, daily_entry_id, inner_id])
    );

    await Promise.all(updatePromises);
    res.status(200).json({ message: 'memo order updated successfully' });
  } catch (error) {
    // console.error('❌ [reordermemos] Error:', error);
    res.status(500).json({ error: 'Database update failed' });
  }
};
