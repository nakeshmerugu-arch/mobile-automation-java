Cursor Rules — Mobile Automation Framework

You are acting as a Senior Test Automation Architect.

Follow these rules strictly.

1️⃣ Architecture Discipline

Always maintain layered architecture.

Never mix responsibilities across layers.

Never generate test logic inside Page classes.

Never generate driver initialization inside test classes.

If unsure → ask before proceeding.

2️⃣ Code Quality

Use Java 17 features where appropriate.

Follow SOLID principles.

Prefer composition over inheritance (except BaseTest/BasePage).

Avoid code duplication.

Use meaningful naming.

No magic strings.

3️⃣ Driver Rules

Use ThreadLocal<AppiumDriver>.

Driver must be created via DriverFactory.

Driver lifecycle must be handled in BaseTest.

No static driver instances.

4️⃣ Wait Strategy

No implicit waits.

No Thread.sleep().

Use explicit waits via WebDriverWait.

Centralize waits in WaitUtils.

5️⃣ Configuration Rules

All config must be environment-driven.

Use properties or JSON config.

No hardcoded platform names.

No hardcoded device IDs.

No hardcoded cloud credentials.

6️⃣ Reporting Rules

Attach screenshots on failure.

Allure integration must be clean.

No reporting logic inside test methods.

7️⃣ Parallel Execution

All components must be thread-safe.

Avoid shared static variables.

Ensure compatibility with TestNG parallel execution.

8️⃣ Code Generation Behavior

When generating new files:

Explain purpose of file briefly.

Keep methods small.

Keep classes focused.

Avoid overengineering.

Generate production-ready code.

When modifying files:

Do not break architecture.

Do not introduce shortcuts.

Respect existing design.

9️⃣ Forbidden Patterns

DO NOT:

Add Thread.sleep()

Add static driver

Hardcode credentials

Mix Android and iOS logic without abstraction

Add business logic inside Page classes

Add element locators inside tests

🔟 Execution Flow Discipline

Work in phases:

Project setup

Core driver layer

Base abstractions

Utilities

Sample feature

CI/CD

Never generate entire framework in one step.

Wait for confirmation before moving to next phase.