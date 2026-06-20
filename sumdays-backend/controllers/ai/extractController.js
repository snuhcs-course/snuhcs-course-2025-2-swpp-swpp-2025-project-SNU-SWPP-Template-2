const axios = require("axios");
const FormData = require("form-data");
const fs = require("fs");
const PYTHON_SERVER_URL = process.env.PYTHON_AI_URL;

const extractController = {
    // Controller method for extracting writing style from diaries (Example: POST /api/ai/extract-style)
    /* POST http://localhost:3000/api/ai/extract-style
    POSTMAN form-data
    images(file) |  multiple files(.jpeg .png etc)
    diaries(text) | ["text1", "text2", ...]
    */
    extractStyle: async (req, res) => {
        try {
            let diaries = [];

            if (req.body.diaries) {
                try {
                    diaries = JSON.parse(req.body.diaries);
                } catch {
                    diaries = req.body.diaries;
                }
            }

            const formData = new FormData();
            formData.append("diaries", JSON.stringify(diaries));

            if (req.files && req.files.length > 0) {
                for (const file of req.files) {
                    formData.append("images", fs.createReadStream(file.path));
                }
            }

            const response = await axios.post(`${PYTHON_SERVER_URL}/extract/style`, formData, {
                headers: {
                    ...formData.getHeaders(),
                },
            });

            if (req.files) {
                for (const file of req.files) {
                    fs.unlink(file.path, (err) => {
                        if (err) console.error("Temp file deletion error:", err);
                    });
                }
            }

            return res.status(200).json({
                success: true,
                style_vector: response.data.style_vector,
                style_examples: response.data.style_examples,
                style_prompt: response.data.style_prompt
            });

        } catch (err) {
            console.error("[extractController.extractStyle] Error:", err.message);
            return res.status(500).json({
                success: false,
                error: err.message,
            });
        }
    },
};

module.exports = extractController;
