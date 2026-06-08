# Selenium Java Project

This repository contains a Maven-based Java + Selenium test suite that automates Amazon shopping-cart flows.
It demonstrates parallel JUnit execution, HTML reporting via ExtentReports, and automatic ChromeDriver management using WebDriverManager.

## Project Structure

- `pom.xml` — Maven build, dependencies, and Surefire configuration.
- `src/test/java/com/example/selenium/AmazonShoppingCartTest.java` — test cases and helpers.
- `target/extent-report/` — generated HTML report and screenshots (created after running tests).

## Prerequisites

- Java 11 or newer (JDK 11+). Verify with:

```bash
java -version
```

- Maven installed (3.6+ recommended). Verify with:

```bash
mvn -v
```

- Google Chrome installed (the tests launch Chrome). WebDriverManager will download a matching ChromeDriver automatically.

Optional (recommended for CI): set `JAVA_HOME` to your JDK installation.

## Setup

1. Clone the repository and open a terminal in the project root (the folder that contains `pom.xml`).

```bash
git clone <repo-url>
cd selenium-java-project
```

2. (Optional) Run a quick compile to confirm the project builds:

```bash
mvn -DskipTests=true test-compile
```

## Running Tests

- Run the full test suite (will create reports in `target/`):

```bash
mvn clean test
```

- Run a single test class:

```bash
mvn -Dtest=AmazonShoppingCartTest test
```

- Run a specific test method (Surefire supports `ClassName#method`):

```bash
mvn -Dtest=AmazonShoppingCartTest#addIphoneToCart test
```

Notes:
- Tests use WebDriverManager; you do not need to manually download ChromeDriver.
- Parallel execution is enabled via JUnit Platform configuration in the project; consult `junit-platform.properties` for tuning.

## Reports & Artifacts

- HTML report: `target/extent-report/index.html`
- Screenshots (on failure): `target/extent-report/screenshots/`
- Surefire XML reports: `target/surefire-reports/` (contains raw test output and system properties)

Open the HTML report in a browser after tests complete:

```bash
start target/extent-report/index.html
```

## Documentation

Project-specific documentation and visual assets are stored under the `docs/` folder:

- `docs/screenshots/` — example screenshots and artifacts collected during test development or runs.
- `docs/google-search-result.svg` — placeholder illustration used in documentation.

Use `docs/` to store any additional run-guides, screenshots, or architecture notes you want to keep outside the main `README.md`.

## Privacy & Cleanup Before Publishing

The generated `target/` folder and test reports may contain local system information (absolute paths, usernames, JVM properties). Do NOT commit `target/` or `target/surefire-reports/` to a public repository.

Before sharing the repository publicly:

```bash
# remove generated artifacts
mvn clean
# (optional) verify no sensitive files remain
git status --porcelain
```

This project already includes `target/` in `.gitignore`, but double-check untracked files before pushing.

## Troubleshooting

- If ChromeDriver/CDP warnings appear in logs, ensure Chrome is up to date or add a matching `selenium-devtools` artifact in `pom.xml` (not required for basic runs).
- If tests fail due to selectors on Amazon changing, open `src/test/java/com/example/selenium/AmazonShoppingCartTest.java` and update the locator strategies.

## Continuous Integration

For CI runs, ensure the build agent has Java and Maven installed and that `DISPLAY`/headless options are configured if necessary. You can run Chrome headless by modifying the WebDriver options in the test class.

## Contact / Next Steps

- If you'd like, I can add a simple `scripts/sanitize-surefire.ps1` script to automatically redact system properties from Surefire XMLs before you publish. Reply `yes` to add it.

Enjoy — run `mvn test` and view `target/extent-report/index.html` for results.
