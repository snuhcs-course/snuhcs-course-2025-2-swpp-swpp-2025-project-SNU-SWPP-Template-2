const express = require('express');
const router = express.Router();
const syncController = require('../../controllers/syncController');

// Local to Server 
router.post('/', syncController.syncData);
router.get('/', syncController.fetchServerData);

module.exports = router;
