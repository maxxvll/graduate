const { spawnSync } = require("node:child_process");

const args = process.argv.slice(2);
process.env.UNI_INPUT_DIR = process.env.UNI_INPUT_DIR || process.cwd();

const isWindows = process.platform === "win32";
const command = isWindows ? "cmd.exe" : "npx";
const commandArgs = isWindows ? ["/d", "/s", "/c", "npx", "uni", ...args] : ["uni", ...args];
const result = spawnSync(command, commandArgs, {
  cwd: process.cwd(),
  env: process.env,
  stdio: "inherit",
});

if (result.error) {
  console.error(result.error);
  process.exit(1);
}

process.exit(typeof result.status === "number" ? result.status : 1);
