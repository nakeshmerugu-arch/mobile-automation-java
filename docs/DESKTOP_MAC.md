# macOS desktop app automation (Appium Mac2)

This guide explains how to run the **desktop** (native macOS `.app`) tests in this repo—e.g. `LaunchDesktopAppTest`—using **Appium 2.x**, the **mac2** driver, and **WebDriverAgent-mac** under the hood.

## What you need

| Requirement | Notes |
|-------------|--------|
| **macOS** | Host machine must be a Mac (Mac2 drives a local app). |
| **Java 17** | Same as the rest of the project (`JAVA_HOME` → 17). |
| **Maven 3.8+** | Run commands from the **repository root**. |
| **Xcode** | Install from the App Store; command-line tools available. |
| **Node.js** | For the **Appium** server (`npm install -g appium` or use `npx`). |
| **Appium 2 + mac2 driver** | See [Install Appium and mac2](#install-appium-and-mac2). |
| **Your `.app` bundle** | Unsigned or ad-hoc builds often need quarantine cleared; see [App bundle and quarantine](#app-bundle-and-quarantine). |

## One-time macOS setup

### Xcode

```bash
xcode-select -p
xcodebuild -version
```

Fix the active developer directory if needed:

```bash
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

### Automation mode (required for Mac2 on recent macOS)

Try **without** `sudo` first (often enough on a normal Mac account):

```bash
/usr/bin/automationmodetool enable-automationmode-without-authentication
```

If macOS prompts for your password and returns **`Authentication failed, incorrect password or not an administrator`**, your account is probably **not an admin** (common on company Macs) or the password was wrong. An **administrator** must run the same command once, or IT must grant admin / run it via MDM.

Only use `sudo` if your environment expects it **and** your user is allowed to run that binary as root:

```bash
sudo /usr/bin/automationmodetool enable-automationmode-without-authentication
```

If you see **`Sorry, user … is not allowed to execute … as root`**, that is a **sudoers / MDM policy** restriction: ask IT to run the command, adjust sudoers, or use a profile that permits it. It is not something the test repo can bypass.

(Validate the tool exists first if you hit `command not found`.)

```bash
ls -l /usr/bin/automationmodetool
```

If the file is missing, verify Xcode is installed and the active developer directory is correct:

```bash
xcode-select -p
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
```

(Reboot may be required after changing this.)

### Accessibility

Grant **Accessibility** (and related) permissions for:

- **Terminal** (or **iTerm**) if you start Appium from the shell  
- **Cursor / VS Code** if the test JVM runs from the IDE  
- Any helper Appium/Xcode uses

Path: **System Settings → Privacy & Security → Accessibility** (and **Screen Recording** if you see prompts).

## Install Appium and mac2

Install Appium 2 (example: global):

```bash
npm install -g appium@latest
appium driver install mac2
appium driver list
```

Start the server (keep this terminal open):

```bash
appium server --address 127.0.0.1 --port 4723
```

Verify the port:

```bash
lsof -nP -iTCP:4723 -sTCP:LISTEN
```

Default URL in config: `http://127.0.0.1:4723`

## App bundle and quarantine

1. Place your **`.app`** where config can point to it, e.g.:

   `apps/desktop-app/YourApp.app`

2. If the app was downloaded or copied from a DMG, macOS may tag it with **quarantine**, which blocks automation launches. The sample test `LaunchDesktopAppTest` can run `xattr -cr` on the bundle when appropriate; you can also clear manually:

   ```bash
   xattr -cr "/path/to/Your App.app"
   ```

3. **Signing**: fully unsigned or broken signatures can cause Mac2/WebDriverAgent-mac launch failures. Use a build that launches normally from Finder first.

## Configuration

Config is loaded from the **classpath** in the `automation` module:

1. `automation/src/test/resources/config/default.properties`  
2. `automation/src/test/resources/config/<env>.properties` (merged on top)

Environment name: JVM property **`env`**, default **`dev`** (**Android**). For desktop runs, set **`-Denv=mac`** so **`config/mac.properties`** is merged (see mobile vs desktop in the repo **README**).

Example overrides:

```bash
-Denv=mac
-Denv=stg
```

### Important properties for Mac (see `mac.properties`)

| Property | Purpose |
|----------|---------|
| `appium.server.url` | Appium base URL, e.g. `http://127.0.0.1:4723` |
| `platformName` | Must be **`Mac`** for desktop |
| `appium:automationName` | **`Mac2`** |
| `appium:appPath` | Path to the `.app` bundle (**relative** to the automation module or **absolute**; spaces: escape with `\ ` in `.properties`) |
| `appium:bundleId` | CFBundleIdentifier (e.g. `com.example.app`) |
| `appium:appWaitForLaunch` | `true` helps wait for the process to come up |
| `appium:skipAppKill` | `false` often helps avoid stale processes between runs |
| `appium:newCommandTimeout` | High enough for heavy WebView startup (e.g. `180`) |

**Note:** For Mac2 you typically set **`appium:appPath`** (not only the generic `app` capability). Align with your `PropertiesDriverConfig` / caps builder.

**Optional JVM flags (Maven `-D…`):**

| Flag | Purpose |
|------|---------|
| `mac.timing.enabled=true` | Per-step timing logs + Allure attachments |
| `mac.loginToPinWaitSeconds` | Seconds to wait after login submit for PIN or home (default **90**, clamped 15–300) |
| `mac.debug.pagesource=true` | On some failure paths, attach full page source (can take **minutes** on large trees) |
| `mac.logout.extraNames=true` | After the main poll, also try `Log out` / `Sign out` / `Sign Out` via `By.name` (default **off**; each call can be slow on mac2) |
| `mac.logout.poolFallback=true` | After `By.name` / loop, scan button + static pools for Keluar/Logout using **few** attribute reads per element (default **off**; each pool still does one `findElements`) |
| `mac.logout.poolOtherNodes=true` | With pool fallback, also scan `XCUIElementTypeOther` (default **off**) |
| `mac.logout.poolScanLimit` | Max elements checked per pool when pool fallback is on (default **40**, clamped 12–60) |

Credentials and secrets should stay in **environment variables** or CI secrets—not committed properties files.

## Build the project

Always build **`core`** when you target **only** the automation module:

```bash
cd /path/to/mobile-automation-java
mvn -q -pl automation -am -DskipTests compile
```

## Optional preflight check

Run the preflight script to validate common Mac2 prerequisites (macOS, Xcode path, `automationmodetool`, Node/npm, Appium/mac2 driver, and port `4723` listener):

```bash
./scripts/mac2-preflight.sh
```

If it reports failures, fix the listed items and re-run until only PASS/WARN remain.

## Run the desktop sample test

The test class is:

`com.mobile.automation.desktop.mac.LaunchDesktopAppTest`

It is **not** included in the default `testng.xml` suite—you run it by **fully qualified class name**.

From **repository root**:

```bash
mvn -pl automation -am -Denv=mac -Dtest=com.mobile.automation.desktop.mac.LaunchDesktopAppTest test
```

Flags:

- **`-pl automation -am`** — builds `core` and runs tests in `automation`.
- **`-Denv=mac`** — loads `config/mac.properties` (Mac2 + `appium:appPath`). The default **`dev`** profile is Android and is wrong for this test.
- **`-Dtest=...`** — single class (Surefire).

If Surefire reports no tests, add:

```bash
-Dsurefire.failIfNoSpecifiedTests=false
```

### JVM / Maven tips

- **JDWP / “Address already in use”**: if Maven forks a JVM with debug options, clear:

  ```bash
  unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS MAVEN_OPTS
  ```

- **Stale Java after Ctrl+Z**: don’t leave `mvn` suspended (`^Z`); use `fg` then Ctrl+C or `kill` the job.

### TestNG `groups`

The sample test may use the group **`mac`**. Running by `-Dtest=` bypasses suite XML group filters for that class; if you use a suite XML that filters by group, include the `mac` group or reference this class explicitly.

## Allure report

Results are written under:

`automation/target/allure-results`

Serve the report (from **`automation`** directory):

```bash
cd automation && mvn allure:serve
```

## Troubleshooting (short)

| Symptom | What to check |
|---------|----------------|
| **Session / launch failed** | App path absolute? Bundle opens in Finder? `xattr -cr`? `bundleId` matches? |
| **“Accessibility not loaded” / empty tree** | Permissions; app foreground; wait longer after launch. |
| **`/usr/bin/automationmodetool: command not found`** | Run `ls -l /usr/bin/automationmodetool`; if missing, install/open Xcode once and re-run `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`, then retry the command. |
| **`Sorry, user … is not allowed to execute … automationmodetool … as root`** | Do not use `sudo` for that user; run `/usr/bin/automationmodetool …` without sudo, or ask IT/admin to enable automation mode / adjust sudoers. |
| **`Authentication failed, incorrect password or not an administrator`** | The tool needs an **admin** Mac user. Confirm **System Settings → Users & Groups** shows **Allow user to administer this computer** for your account, or ask IT to run `automationmodetool` once / add you to admins. Wrong password also produces this message. |
| **Very slow PIN or update steps** | Avoid committing huge blobs; tests use element-first waits—ensure `getPageSource()` isn’t in a tight custom loop. |
| **`completePinAndReachHome` many minutes then PIN timeout** | Often **Allure `getPageSource()` on failure** (huge WebView XML). Failures now use a light attachment by default; use `-Dmac.debug.pagesource=true` only when you need XML. Increase wait: `-Dmac.loginToPinWaitSeconds=120`. |
| **Git push timeout** | Don’t commit `.app` / `.dmg` / `node_modules`; keep binaries out of Git or use Git LFS. |

## Related files

- Page object: `automation/src/main/java/com/mobile/automation/desktop/mac/pages/MainWindowPage.java`
- Sample test: `automation/src/test/java/com/mobile/automation/desktop/mac/LaunchDesktopAppTest.java`
- Sample config: `automation/src/test/resources/config/dev.properties`
- Architecture: [ARCHITECTURE_CONTEXT.md](ARCHITECTURE_CONTEXT.md)
