module.exports = {
  testEnvironment: "node",
  coveragePathIgnorePatterns: [
    'middlewares/authMiddleware.js'   
  ],
  testMatch: [
    "**/tests/**/*.test.js",
    "**/tests/**/*.e2e.js"
  ],
  setupFilesAfterEnv: ["<rootDir>/tests/e2e/setup.js"],
  testTimeout: 30000, // Flask + OpenAI 응답 고려 여유
  verbose: true
};
