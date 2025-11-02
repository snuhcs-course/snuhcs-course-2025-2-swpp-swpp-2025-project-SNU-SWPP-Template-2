const baseConfig = require("./jest.config.js")

module.exports = {
  ...baseConfig,
  collectCoverage: true,
  collectCoverageFrom: ["app/models/**/*.{ts,tsx}", "!**/node_modules/**"],
}