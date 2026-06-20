// middlewares/authMiddleware.js

const jwt = require('jsonwebtoken');
const { pool } = require('../db/db');

const JWT_SECRET =
  process.env.JWT_SECRET || 'Sumdays_Project_Super_Secret_Key_!@#$%^&*()';

module.exports = async function authMiddleware(req, res, next) {
  const authHeader = req.headers['authorization'];

  req.user = null;

  if (!authHeader) {
    return next();
  }

  const parts = authHeader.split(' ');

  if (parts.length !== 2 || parts[0] !== 'Bearer') {
    return next();
  }

  const token = parts[1];

  try {
    const decoded = jwt.verify(token, JWT_SECRET);

    const [rows] = await pool.query(
      'SELECT id FROM users WHERE id = ?',
      [decoded.userId]
    );

    if (rows.length === 0) {
      return next();
    }

    req.user = decoded;
    return next();

  } catch (err) {
    console.error('⚠️ [JWT 검증 실패]', err.message);
    req.user = null;
    return next();
  }
};