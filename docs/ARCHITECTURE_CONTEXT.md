Project: Enterprise Mobile Automation Framework

This repository is a production-grade, scalable, enterprise-level Mobile Automation Framework.

Primary Goals

Android + iOS support

Appium 2.x based

Java 17

Maven

TestNG

Allure Reporting

Thread-safe parallel execution

CI/CD ready

Cloud-ready (BrowserStack support)

Environment-based execution (dev/stg/prod)

This is NOT a demo project.
This is NOT a learning project.
This must follow enterprise design principles.

Architecture Principles

We follow layered architecture:

Test Layer → Flow Layer → Page Layer → Core Engine

Layer Responsibilities

Tests → Contain only assertions & high-level scenario intent

Flows → Business-level reusable actions

Pages → Element interaction only

Driver Layer → Driver lifecycle management

Utils → Generic reusable utilities

Config → Environment-driven configuration

Tests must NEVER directly interact with page elements.

Technical Standards

No Thread.sleep()

No static driver

Use ThreadLocal driver

All waits must use WebDriverWait

No hardcoded credentials

No hardcoded URLs

No duplication

Follow SOLID principles

Use logging (Log4j2)

Use proper exception handling

Everything must be extensible

Parallel Execution Requirement

Framework must support parallel execution

Thread-safe driver handling

No shared mutable state

Future Extensibility

Framework must support:

API automation layer

Visual validation

Figma comparison engine

Cloud execution

CI/CD integration

Architecture decisions must allow extension without refactoring core layers.

## Multi-module structure

- **mobile-automation-parent** (root): dependency management, plugin management, Java 17.
- **core**: `config`, `driver`, `utils` packages; Log4j2; no TestNG/Allure.
- **automation**: `pages`, `flows`, `tests`; depends on core; TestNG, Allure, Appium client.

## Driver layer (Phase 2)

- **DriverFactory**: Creates `AndroidDriver`, `IOSDriver`, or `Mac2Driver` from `URL` + `Capabilities`. Platform is taken from capability `platformName` (or `appium:platformName`). No static driver.
- **DriverManager**: Singleton that holds `ThreadLocal<AppiumDriver>`. Use `getInstance()` then `initDriver(URL, Capabilities)` or `initDriver(String, Capabilities)` to create and set the driver for the current thread; `getDriver()` to retrieve; `quitDriver()` to quit and clear. `BaseTest` calls `initDriver` from the first `getDriver()` in a test and `quitDriver` from `@AfterMethod`.

## Base abstractions (Phase 3)

- **DriverConfig** (core): Interface for Appium server URL and capabilities. **PropertiesDriverConfig** builds from `Properties`; required keys: `appium.server.url`, `platformName`; other keys become capabilities.
- **DriverConfigLoader** (automation test): Loads config from classpath `config/default.properties` and `config/{env}.properties` (env from system property `env` or `dev`). Returns `Optional<DriverConfig>`; if required keys are missing, driver is not initialized so tests without a device can still run.
- **BaseTest** (automation test): Lazily inits the driver on first `getDriver()` when `DriverConfigLoader` returns config (runs `preInitDriver` before session start); `@AfterMethod` quits and attaches screenshot on failure (Allure). Tests that never call `getDriver()` skip Appium (e.g. smoke tests). Tests extend BaseTest and use `getDriver()` rather than constructing drivers directly.
- **BasePage** (automation main): Constructor takes `AppiumDriver`; exposes `getDriver()` and `waitUtils()` for subclasses. Page layer uses it for element interaction only.

## Utilities (Phase 4)

- **WaitUtils** (core): Centralized explicit waits. Uses `WebDriverWait` / `FluentWait` only; no `Thread.sleep()` or implicit waits. Construct with `WebDriver` and optional `Duration` timeout (default 15s) and poll interval (default 500ms). Methods: `waitUntilPresent(By)`, `waitUntilVisible(By)`, `waitUntilClickable(By)`, `waitUntilInvisible(By)`, and overloads for `WebElement`. Use from page classes before interacting with elements.
- **BasePage**: Exposes `waitUtils()` that returns a `WaitUtils` instance for the page's driver (lazy-initialized).

## Sample feature (Phase 5)

- **HomePage** (automation pages): First screen after launch. Waits for main content (`android:id/content`) via `waitUntilContentVisible()` using WaitUtils. Element interaction only.
- **AppReadyFlow** (automation flows): Business action "app is ready". Uses HomePage to wait for content; no direct element interaction.
- **SampleFeatureTest** (automation tests): Extends BaseTest, creates AppReadyFlow(getDriver()), runs flow, attaches screenshot, asserts. No direct page or element access; demonstrates Test → Flow → Page layering.