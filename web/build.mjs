import { cpSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";

const token = process.env.POSTHOG_WEB_PROJECT_TOKEN?.trim() ?? "";
if (token && !/^phc_[a-zA-Z0-9_-]+$/.test(token)) {
  throw new TypeError("POSTHOG_WEB_PROJECT_TOKEN must be a public PostHog project token.");
}

const environment = process.env.VERCEL_ENV === "production" ? "production" : "development";
const html = readFileSync("index.html", "utf8")
  .replace('name="posthog-project-token" content=""', `name="posthog-project-token" content="${token}"`)
  .replace('name="analytics-environment" content="development"', `name="analytics-environment" content="${environment}"`);

mkdirSync("dist", { recursive: true });
writeFileSync("dist/index.html", html);
for (const path of ["assets", "analytics.js", "invitation.js", "page.js", "style.css"]) {
  cpSync(path, `dist/${path}`, { recursive: true });
}
