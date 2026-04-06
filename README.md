# Mobile Automation Framework

Enterprise mobile automation framework: **Appium 2.x**, **Java 17**, **Maven**, **TestNG**, **Allure**. Android and iOS support with layered architecture and thread-safe parallel execution.

See [docs/ARCHITECTURE_CONTEXT.md](docs/ARCHITECTURE_CONTEXT.md) for architecture and principles.

## Multi-module layout

| Module       | Purpose |
|-------------|---------|
| **core**    | Driver lifecycle, configuration contract, shared utilities. No test dependencies. |
| **automation** | Pages, flows, tests. Depends on `core`; uses TestNG and Allure. |

Layers: **Tests → Flows → Pages → Core** (driver, config, utils).

## Prerequisites

- Java 17
- Maven 3.8+

## Build and test

```bash
# Compile everything
mvn clean compile

# Run tests (automation module; run from root)
mvn clean test

# Generate and open Allure report (run from automation directory)
cd automation && mvn allure:serve
```

From root, `mvn clean test` runs the automation module's tests. Allure results are under `automation/target/allure-results`. Run `mvn allure:serve` from the `automation` directory to view the report.

**Run a single test** (e.g. `LaunchAppTest`): build the `core` dependency and run only that test:

```bash
mvn test -pl automation -am -Dtest=LaunchAppTest -Dsurefire.failIfNoSpecifiedTests=false
```

Always run from the **project root**. Using `-pl automation` alone skips building `core`, which will cause "Could not find artifact com.mobile.automation:core".

## App binaries (APK / IPA)

Put Android (`.apk`) and iOS (`.ipa` or `.app`) files in the **`apps/`** folder at the repo root:

```
mobile-automation-java/
  apps/
    MyApp.apk      # Android
    MyApp.ipa      # iOS (or .app for simulator)
```

Reference them in config via the `app` capability (path can be relative to project root or absolute):

- In `automation/src/test/resources/config/dev.properties` (or your env file):
  - `app=apps/MyApp.apk` (Android)
  - `app=apps/MyApp.ipa` (iOS)

`apps/*.apk` and `apps/*.ipa` are in `.gitignore` so large binaries are not committed. Use CI to copy builds into `apps/` or point `app` to a URL/artifact.

## Configuration

- Config is environment-driven (dev/stg/prod). No hardcoded URLs or credentials.
- Placeholder config: `automation/src/test/resources/config/` (e.g. `default.properties`, `dev.properties`).
- Credentials and device IDs must come from environment variables or CI secrets.

## Phases

1. **Phase 1** – Project setup (multi-module layout) ✓  
2. **Phase 2** – Core driver layer (DriverFactory, DriverManager with ThreadLocal) ✓  
3. **Phase 3** – Base abstractions (BaseTest, BasePage, DriverConfig, config loading) ✓  
4. **Phase 4** – Utilities (WaitUtils with WebDriverWait, no Thread.sleep()) ✓  
5. **Phase 5** – Sample feature (HomePage, AppReadyFlow, SampleFeatureTest) ✓  
6. CI/CD  

Do not commit secrets. Wait for confirmation before moving to the next phase.

## Desktop (macOS) app automation

To run **native `.app`** tests with **Appium Mac2** (sample: `LaunchDesktopAppTest`), see the full guide:

**[docs/DESKTOP_MAC.md](docs/DESKTOP_MAC.md)** — prerequisites, Xcode/automation mode, Appium + mac2 install, `dev.properties` capabilities, Maven command, Allure, troubleshooting.

Quick run (from repo root, Appium already listening on `4723`):

```bash
mvn -pl automation -am -Dtest=com.mobile.automation.desktop.mac.LaunchDesktopAppTest test
```
