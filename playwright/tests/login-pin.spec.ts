import { _electron as electron, expect, test, type ElectronApplication, type Page } from "@playwright/test";
import { existsSync } from "node:fs";
import { join } from "node:path";
import { execFileSync } from "node:child_process";

const APP_BUNDLE_PATH =
  process.env.PW_DESKTOP_APP_PATH ??
  "/Users/nakesh.merugu/Automation/mobile-automation-java/apps/desktop-app/Ajaib Desktop Web Trade.app";
const EMAIL = process.env.PW_EMAIL ?? "";
const PASSWORD = process.env.PW_PASSWORD ?? "";
const PIN = process.env.PW_PIN ?? "1234";
const WINDOW_DISCOVERY_TIMEOUT_MS = 60_000;

function resolveMacAppExecutable(appBundlePath: string): string {
  if (!existsSync(appBundlePath)) {
    throw new Error(`Desktop app bundle not found: ${appBundlePath}`);
  }
  const infoPlist = join(appBundlePath, "Contents", "Info.plist");
  const executableName = execFileSync(
    "/usr/libexec/PlistBuddy",
    ["-c", "Print :CFBundleExecutable", infoPlist],
    { encoding: "utf8" },
  ).trim();
  if (!executableName) {
    throw new Error(`CFBundleExecutable not found in ${infoPlist}`);
  }
  const executablePath = join(appBundlePath, "Contents", "MacOS", executableName);
  if (!existsSync(executablePath)) {
    throw new Error(`Desktop app executable not found: ${executablePath}`);
  }
  return executablePath;
}

function emailLocator(page: Page) {
  return page.locator("input[type='email'], input[name*='email' i], input[id*='email' i]").first();
}

function passwordLocator(page: Page) {
  return page
    .locator("input[type='password'], input[name*='password' i], input[id*='password' i]")
    .first();
}

async function pickLoginWindow(app: ElectronApplication): Promise<Page> {
  const deadline = Date.now() + WINDOW_DISCOVERY_TIMEOUT_MS;
  while (Date.now() < deadline) {
    let windows = app.windows();
    if (windows.length === 0) {
      try {
        await app.waitForEvent("window", { timeout: 2_000 });
      } catch {
        // No new window event yet.
      }
      windows = app.windows();
    }
    for (const w of windows) {
      try {
        await w.waitForLoadState("domcontentloaded", { timeout: 1_000 });
        const hasPassword = (await passwordLocator(w).count()) > 0;
        if (hasPassword) {
          return w;
        }
      } catch {
        // Keep scanning windows until timeout.
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error("No window with login password input found within timeout.");
}

test("desktop app login and submit 4-digit pin", async () => {
  if (!EMAIL || !PASSWORD) {
    test.skip(true, "Set PW_EMAIL and PW_PASSWORD environment variables.");
  }

  const executablePath = resolveMacAppExecutable(APP_BUNDLE_PATH);
  const app = await electron.launch({
    executablePath,
  });

  try {
    const page = await pickLoginWindow(app);
    await page.screenshot({ path: "test-results/desktop-first-window.png" });

    await expect(passwordLocator(page)).toBeVisible({
      timeout: 30_000,
    });

    await emailLocator(page).fill(EMAIL);
    await passwordLocator(page).fill(PASSWORD);
    await page.screenshot({ path: "test-results/desktop-after-credentials.png" });
    await page.getByRole("button", { name: /masuk|login|sign in/i }).first().click();

    for (let i = 0; i < 4; i += 1) {
      await page.locator(`#pin-input-${i}`).fill(PIN.charAt(i));
    }
    await page.screenshot({ path: "test-results/desktop-after-pin.png" });
    await page.getByRole("button", { name: /^submit$/i }).click();

    await expect(page.locator("#pin-input-0")).toBeHidden({ timeout: 20_000 });
  } finally {
    await app.close();
  }
});
