const axios = require("axios");
const FormData = require("form-data");
const fs = require("fs");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const ocrController = {
    // Controller method for OCR on a memo image (Example: POST /api/ai/ocr/memo)
    /* POST http://localhost:3000/api/ai/ocr/memo
    POSTMAN form-data
    image file | .jpeg .png etc
    type text | extract || describe
    */
    memo: async (req, res) => {
        try {
            if (!req.file) {
                return res.status(400).json({ error: "No image file uploaded." });
            }

            const analysisType = req.body.type;
            if (!["extract", "describe"].includes(analysisType)) {
                return res.status(400).json({ error: "Invalid analysis type." });
            }

            const formData = new FormData();
            formData.append("image", fs.createReadStream(req.file.path));
            formData.append("type", analysisType);

            const response = await axios.post(`${PYTHON_SERVER_URL}/image/memo`, formData, {
                headers: {
                    ...formData.getHeaders(),
                },
            });

            fs.unlink(req.file.path, (err) => {
                if (err) console.error("Temp file deletion error:", err);
            });

            res.status(200).json({
                success: true,
                type: response.data.type,
                result: response.data.text,
            });
        } catch (error) {
            console.error("[imageController.memo] Error:", error.message);
            res.status(500).json({
                error: error.message,
            });
        }
    },

    // Controller method for OCR on a diary image (Example: POST /api/ai/ocr/diary)
    /* POST http://localhost:3000/api/ai/ocr/diary
    POSTMAN form-data
    image file | .jpeg .png etc
    */
    diary: async (req, res) => {
        try {
            const imageFiles = req.files;
            if (!imageFiles || imageFiles.length === 0) {
                return res.status(400).json({ error: "No image files uploaded." });
            }

            const formData = new FormData();
            for (const imageFile of imageFiles) {
                formData.append("image", fs.createReadStream(imageFile.path));
            }

            const response = await axios.post(`${PYTHON_SERVER_URL}/image/diary`, formData, {
                headers: {
                    ...formData.getHeaders(),
                },
            });

            for (const file of req.files) {
                fs.unlink(file.path, (err) => {
                    if (err) console.error("Temp file deletion error:", err);
                });
            }

            res.status(200).json({
                success: true,
                result: response.data.result,
            });
        } catch (error) {
            console.error("[imageController.diary] Error:", error.message);
            res.status(500).json({
                error: error.message,
            });
        }
    },
};

module.exports = ocrController;