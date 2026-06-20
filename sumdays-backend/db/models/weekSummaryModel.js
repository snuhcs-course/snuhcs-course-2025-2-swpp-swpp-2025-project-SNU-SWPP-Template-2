// models/weekSummaryModel.js

const { pool } = require('../db');

async function upsertWeekSummary({
  userId,
  startDate,
  endDate,
  diaryCount,
  emotionAnalysis,
  highlights,
  insights,
  summary
}) {
  const [result] = await pool.query(
    `
    INSERT INTO week_summary (
      user_id,
      startDate,
      endDate,
      diaryCount,
      emotionAnalysis,
      highlights,
      insights,
      summary
    )
    VALUES (?, ?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), CAST(? AS JSON), CAST(? AS JSON))
    ON DUPLICATE KEY UPDATE
      endDate = VALUES(endDate),
      diaryCount = VALUES(diaryCount),
      emotionAnalysis = VALUES(emotionAnalysis),
      highlights = VALUES(highlights),
      insights = VALUES(insights),
      summary = VALUES(summary)
    `,
    [
      userId,
      startDate,
      endDate,
      diaryCount,
      JSON.stringify(emotionAnalysis),
      JSON.stringify(highlights),
      JSON.stringify(insights),
      JSON.stringify(summary)
    ]
  );

  return result;
}

module.exports = {
  upsertWeekSummary
};