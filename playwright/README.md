# Playwright desktop comparison check

This folder is a standalone Playwright setup to validate the macOS desktop app flow outside Appium Mac2.
It launches the `.app` bundle directly through Playwright's Electron support.

## Setup

```bash
cd playwright
npm install
npx playwright install
```

## Run

```bash
PW_DESKTOP_APP_PATH="/Users/nakesh.merugu/Automation/mobile-automation-java/apps/desktop-app/Ajaib Desktop Web Trade.app" \
PW_EMAIL="user@example.com" \
PW_PASSWORD="your-password" \
PW_PIN="1234" \
npm run test:headed
```

Notes:
- The test uses `#pin-input-0` to `#pin-input-3` and clicks the `Submit` button.
- It resolves the actual app executable from `Info.plist` (`CFBundleExecutable`) and launches the bundle executable under `Contents/MacOS/`.
- This approach works for Electron-based macOS desktop apps.
