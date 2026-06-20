require("dotenv").config();
const app = require("./app");

const port = process.env.NODEJS_SERVER_PORT;

app.listen(port, "0.0.0.0", () => {
  console.log(`Sumdays app listening on port ${port}`);
  console.log("Waiting for request...");
});
