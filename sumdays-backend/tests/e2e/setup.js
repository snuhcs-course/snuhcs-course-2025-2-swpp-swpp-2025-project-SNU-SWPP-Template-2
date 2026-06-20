const { spawn } = require("child_process");

let flask;

beforeAll(async () => {
  flask = spawn("python", ["app.py"], {
    cwd: "./ai",
    stdio: "inherit",
    env: { ...process.env }
  });

  // Flask 초기화 대기
  await new Promise(resolve => setTimeout(resolve, 3000));
});

afterAll(() => {
  if (flask) flask.kill("SIGINT");
});
