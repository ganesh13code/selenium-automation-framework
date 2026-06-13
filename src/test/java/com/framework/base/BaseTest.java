package com.framework.base;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.framework.utils.ConfigReader;
import com.framework.utils.DriverManager;
import com.framework.utils.ExtentReportManager;
import com.framework.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.time.Duration;

/**
 * BaseTest — The parent class that ALL test classes extend.
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: TestNG Lifecycle Annotations (execution order)
 * ═══════════════════════════════════════════════════════════
 *
 * @BeforeSuite    Runs ONCE before the entire test suite starts.
 *                 → Initialize report, DB connections, global config.
 *
 * @BeforeClass    Runs ONCE before all tests in a particular class.
 *                 → Rarely needed; prefer @BeforeMethod.
 *
 * @BeforeMethod   Runs before EACH @Test method.
 *                 → Launch browser, navigate to base URL.
 *                 → Receives ITestResult to know the test name.
 *
 * @AfterMethod    Runs after EACH @Test method (always, even on failure).
 *                 → Capture screenshot on failure, log result, quit browser.
 *                 → alwaysRun=true ensures it runs even if @BeforeMethod failed.
 *
 * @AfterClass     Runs ONCE after all tests in a class finish.
 *
 * @AfterSuite     Runs ONCE after all tests in the suite finish.
 *                 → Flush report, close connections.
 *
 * Execution order for two tests:
 *   @BeforeSuite → @BeforeMethod → test1 → @AfterMethod
 *                → @BeforeMethod → test2 → @AfterMethod → @AfterSuite
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: ITestResult
 * ═══════════════════════════════════════════════════════════
 * TestNG injects ITestResult into @AfterMethod automatically.
 * It carries the outcome of the test just executed:
 *   ITestResult.SUCCESS  (1) — test passed
 *   ITestResult.FAILURE  (2) — test threw an exception or assertion failed
 *   ITestResult.SKIP     (3) — test was skipped (dependency failed, @Test(enabled=false))
 *
 * We use it to decide: "take screenshot only on FAILURE".
 * ═══════════════════════════════════════════════════════════
 */
public class BaseTest {

    protected static final Logger log = LogManager.getLogger(BaseTest.class);

    // ─────────────────────────────────────────────────────
    // Suite-level lifecycle
    // ─────────────────────────────────────────────────────

    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() {
        log.info("╔══════════════════════════════════╗");
        log.info("║     TEST SUITE STARTING          ║");
        log.info("╚══════════════════════════════════╝");
        ExtentReportManager.initReports();
    }

    @AfterSuite(alwaysRun = true)
    public void suiteTearDown() {
        ExtentReportManager.flushReports();
        log.info("╔══════════════════════════════════╗");
        log.info("║     TEST SUITE FINISHED          ║");
        log.info("╚══════════════════════════════════╝");
    }

    // ─────────────────────────────────────────────────────
    // Method-level lifecycle (per test)
    // ─────────────────────────────────────────────────────

    /**
     * Runs before each @Test method.
     * TestNG injects ITestResult so we can read the test method name.
     *
     * @param result injected by TestNG — contains test metadata
     */
    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestResult result) {
        String testName = result.getMethod().getMethodName();
        log.info("┌─ Starting: {}", testName);

        // 1. Create an ExtentTest node for this test in the report
        ExtentReportManager.createTest(testName);
        ExtentReportManager.getTest().log(Status.INFO, "Test started: " + testName);

        // 2. Launch browser (creates a new ThreadLocal WebDriver for this thread)
        DriverManager.initDriver(ConfigReader.getBrowser(), ConfigReader.isHeadless());

        // 3. Configure timeouts on the new driver
        WebDriver driver = DriverManager.getDriver();
        driver.manage().timeouts()
              .implicitlyWait(Duration.ofSeconds(ConfigReader.getImplicitWait()));
        driver.manage().timeouts()
              .pageLoadTimeout(Duration.ofSeconds(ConfigReader.getPageLoadTimeout()));

        // 4. Navigate to the starting URL
        String url = ConfigReader.getBaseUrl();
        driver.get(url);
        log.info("  Navigated to: {}", url);
        ExtentReportManager.getTest().log(Status.INFO, "Navigated to: " + url);
    }

    /**
     * Runs after each @Test method — ALWAYS, even if the test failed.
     * alwaysRun=true is critical: without it, a failed test skips teardown
     * and leaves the browser open (and ThreadLocal dirty for the next test).
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        try {
            if (result.getStatus() == ITestResult.FAILURE) {
                log.error("└─ FAILED: {}", testName);
                handleFailure(result, testName);

            } else if (result.getStatus() == ITestResult.SUCCESS) {
                log.info("└─ PASSED: {}", testName);
                ExtentReportManager.getTest().pass("Test passed ✓");

            } else if (result.getStatus() == ITestResult.SKIP) {
                log.warn("└─ SKIPPED: {}", testName);
                ExtentReportManager.getTest().skip("Skipped — " +
                        (result.getThrowable() != null ? result.getThrowable().getMessage() : ""));
            }
        } finally {
            // ALWAYS quit driver — even if logging throws an exception
            DriverManager.quitDriver();
        }
    }

    /**
     * Handles failure: logs the exception and embeds a screenshot in the report.
     */
    private void handleFailure(ITestResult result, String testName) {
        WebDriver driver = DriverManager.getDriver();

        // Capture screenshot as Base64 (for report embedding)
        String base64 = ScreenshotUtils.captureScreenshotAsBase64(driver);

        // Also save to disk (for CI artifact upload)
        ScreenshotUtils.captureScreenshot(driver, testName);

        if (base64 != null) {
            ExtentReportManager.getTest().fail(
                    result.getThrowable(),
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build()
            );
        } else {
            // Fallback: log failure without screenshot
            ExtentReportManager.getTest().fail(result.getThrowable());
        }
    }

    // ─────────────────────────────────────────────────────
    // Protected helpers for subclasses
    // ─────────────────────────────────────────────────────

    /**
     * Subclasses (test classes) call getDriver() to access the active WebDriver.
     * Delegates to DriverManager.getDriver() which returns the ThreadLocal instance.
     */
    protected WebDriver getDriver() {
        return DriverManager.getDriver();
    }
}
