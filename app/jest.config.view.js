const baseConfig = require("./jest.config.js")

module.exports = {
  ...baseConfig,
  collectCoverage: true,
  collectCoverageFrom: [
    "app/components/**/*.{ts,tsx}",
    "app/screens/**/*.{ts,tsx}",
    "app/navigators/**/*.{ts,tsx}",
    "!**/node_modules/**",
  ],
}