const axios = require("axios");
const FormData = require("form-data");
const fs = require("fs");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const sttController = {
    // Controller method for STT on a memo (Example: POST /api/ai/stt/memo)
    /* POST http://localhost:3000/api/ai/stt/memo
    POSTMAN form-data
    */
    memo: async (req, res) => {
        try {
            if (!req.file) {
                return res.status(400).json({ error: "No audio file uploaded." });
            }

            const formData = new FormData();
            formData.append("audio", fs.createReadStream(req.file.path));

            const response = await axios.post(`${PYTHON_SERVER_URL}/stt/memo`, formData, {
                headers: {
                    ...formData.getHeaders(),
                },
            });

            fs.unlink(req.file.path, (err) => {
                if (err) console.error("Temp file deletion error:", err);
            });

            res.status(200).json({
                success: true,
                transcribed_text: response.data.transcribed_text,
            });
        } catch (error) {
            console.error("[sttController.merge] Error:", error.message);
            res.status(500).json({
                success: false,
                error: error.message,
            });
        }
    },
};

module.exports = sttController;
