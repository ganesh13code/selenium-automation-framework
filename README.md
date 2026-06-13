# Hybrid Test Automation Framework

A Selenium + TestNG + Maven framework using the **Page Object Model**, **ThreadLocal WebDriver** for parallel execution, **ExtentReports** for HTML reporting, and **data-driven testing** via TestNG `@DataProvider`.

## Prerequisites

- Java JDK 17 (`java -version`)
- Maven 3.6+ (`mvn -version`)
- Google Chrome installed (default browser — WebDriverManager auto-downloads the driver)

## Project Structure

```
HybridFramework
├── pom.xml                     # Maven dependencies & build config
├── testng.xml                  # Test suite definition (smoke / regression)
└── src/test
    ├── java/com/framework
    │   ├── base/BaseTest.java       # TestNG lifecycle (@Before/AfterSuite/Method)
    │   ├── pages/
    │   │   ├── BasePage.java        # Shared Selenium actions (click, type, wait...)
    │   │   └── LoginPage.java       # Page Object for the Login page
    │   ├── tests/LoginTest.java     # Actual test cases
    │   └── utils/
    │       ├── ConfigReader.java        # Reads config.properties
    │       ├── DriverManager.java       # ThreadLocal WebDriver factory
    │       ├── WaitUtils.java           # Explicit/Fluent wait wrappers
    │       ├── ScreenshotUtils.java     # Screenshot on failure
    │       └── ExtentReportManager.java # HTML report (Singleton + ThreadLocal)
    └── resources
        ├── config.properties    # browser, URL, timeouts, headless flag
        └── log4j2.xml           # logging config
```

## How to Run

1. Extract the zip and `cd` into `HybridFramework`.
2. Run the full suite (as defined in `testng.xml`):
   ```bash
   mvn clean test
   ```
3. Run only the smoke group:
   ```bash
   mvn clean test -Dgroups=smoke
   ```
4. Run headless (e.g. for CI):
   - Edit `src/test/resources/config.properties` → set `headless=true`
   - Or pass at runtime (requires reading system property in ConfigReader — currently file-based)

## Outputs

After running, check:
- `test-output/reports/HybridFrameworkReport.html` — interactive ExtentReports HTML report
- `test-output/screenshots/` — PNG screenshots captured on test failure
- `test-output/logs/automation.log` — full execution log

Open the HTML report in any browser to see pass/fail status, execution time, system info, and embedded failure screenshots.

## How the Layers Connect

```
LoginTest (extends BaseTest)
  → BaseTest.@BeforeMethod launches browser via DriverManager (ThreadLocal)
  → Test creates LoginPage(getDriver())
  → LoginPage (extends BasePage) uses @FindBy locators + inherited actions
  → BasePage actions use WaitUtils for stable interactions
  → ConfigReader supplies browser/URL/timeout values to everything
  → BaseTest.@AfterMethod captures screenshot on failure (ScreenshotUtils)
    and logs result to ExtentReportManager
  → BaseTest.@AfterSuite flushes the HTML report
```

## Adding a New Page/Test

1. Create `pages/YourPage.java` extends `BasePage`, define `@FindBy` locators + action methods (return `this` or the next page for chaining).
2. Create `tests/YourTest.java` extends `BaseTest`, write `@Test` methods using AAA (Arrange-Act-Assert).
3. Add the class to `testng.xml` under `<classes>`.

## Switching Environments

Edit `src/test/resources/config.properties`:
```properties
base.url=https://your-qa-url.com
browser=chrome
headless=true
```
For multiple environments, duplicate this file (`config-qa.properties`, `config-prod.properties`) and extend `ConfigReader` to load based on a `-Denv=` system property.

## Sample Test Credentials

The included `LoginTest` targets the public demo site `https://the-internet.herokuapp.com/login`:
- Valid: `tomsmith` / `SuperSecretPassword!`
- Any other combination triggers the error flash message.
