const baseConfig = require("./jest.config.js")

module.exports = {
  ...baseConfig,
  collectCoverage: true,
  collectCoverageFrom: ["app/services/**/*.{ts,tsx}", "!**/node_modules/**"],
}