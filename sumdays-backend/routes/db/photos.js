const express = require('express');
const router = express.Router();
const photosController = require('../../controllers/photosController');

// ✅ 1️⃣ 사진 생성
router.post('/:date', photosController.createPhoto);

// ✅ 2️⃣ 사진 삭제
router.delete('/:date/:inner_id', photosController.deletePhoto);

// ✅ 3️⃣ 사진 순서 변경
router.patch('/:date/order', photosController.reorderPhotos);

module.exports = router;
