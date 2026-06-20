const express = require('express');
const router = express.Router();
const dailyController = require('../../controllers/dailyController');

// ✅ 1️⃣ 특정 날짜 일기 조회 (전체, include, exclude 모두 지원)
router.get('/:date', dailyController.getDailyEntry);

// ✅ 2️⃣ 새 일기 생성
router.post('/', dailyController.createDailyEntry);

// ✅ 3️⃣ 특정 날짜 일기 수정 (icon_name, diary, ai_comment 등)
router.patch('/:date', dailyController.updateDailyEntry);

// ✅ 4️⃣ 특정 날짜 일기 삭제
router.delete('/:date', dailyController.deleteDailyEntry);

module.exports = router;
