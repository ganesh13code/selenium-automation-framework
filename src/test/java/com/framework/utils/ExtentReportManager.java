package com.framework.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * ExtentReportManager — Manages HTML test execution reports.
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Two separate objects — understand the difference
 * ═══════════════════════════════════════════════════════════
 *
 * ExtentReports (Singleton)
 *   → The REPORT FILE. One instance for the entire test run.
 *   → Shared across all threads.
 *   → Created in @BeforeSuite, flushed in @AfterSuite.
 *   → flush() actually writes the HTML file to disk.
 *
 * ExtentTest (ThreadLocal)
 *   → A SINGLE TEST NODE inside the report.
 *   → Each test method gets its own node.
 *   → Must be ThreadLocal — in parallel execution, Thread-1's
 *     logs should not appear in Thread-2's test node.
 *   → Created in @BeforeMethod, used in @AfterMethod.
 *
 * Rule of thumb:
 *   One report, many tests. One test node per thread. Both patterns needed.
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Singleton Pattern
 * ═══════════════════════════════════════════════════════════
 * ExtentReports is initialized once (initReports()) and reused everywhere.
 * If multiple threads tried to create their own ExtentReports, each would
 * overwrite the same output file — producing a corrupt or empty report.
 * The single shared instance accumulates ALL test results safely because
 * ExtentReports itself handles concurrent writes via synchronization.
 * ═══════════════════════════════════════════════════════════
 */
public class ExtentReportManager {

    private static final Logger log = LogManager.getLogger(ExtentReportManager.class);

    // SINGLETON: one report for the whole suite
    private static ExtentReports extentReports;

    // THREADLOCAL: one test node per thread
    private static final ThreadLocal<ExtentTest> extentTestThreadLocal = new ThreadLocal<>();

    private ExtentReportManager() {}

    /**
     * Creates and configures the ExtentReports instance.
     * Must be called ONCE before any test starts — in @BeforeSuite.
     */
    public static synchronized void initReports() {
        String reportDir  = ConfigReader.getReportDir();

        String baseReportName = ConfigReader.getReportName();
        String timestamp = getTimestamp();

        String reportName = baseReportName + "_" + timestamp;
        String reportPath = reportDir + File.separator + reportName + ".html";

        // Create output directory if it doesn't exist
        File dir = new File(reportDir);
        if (!dir.exists()) dir.mkdirs();

        // ExtentSparkReporter generates the visual HTML report
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle(reportName);
        sparkReporter.config().setReportName("Hybrid Framework — Execution Report");
        sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);

        // System info panel shown at the top of the report
        extentReports.setSystemInfo("OS",          System.getProperty("os.name"));
        extentReports.setSystemInfo("Java",        System.getProperty("java.version"));
        extentReports.setSystemInfo("Browser",     ConfigReader.getBrowser());
        extentReports.setSystemInfo("Environment", "QA");
        extentReports.setSystemInfo("Base URL",    ConfigReader.getBaseUrl());

        log.info("ExtentReports initialized → {}", reportPath);
    }

    /**
     * Creates a new test node in the report for the given test.
     * Each test method calls this in @BeforeMethod.
     * Stored in ThreadLocal so parallel tests log to their own node.
     *
     * @param testName name displayed in the report (usually method name + description)
     */
    public static void createTest(String testName) {
        ExtentTest test = extentReports.createTest(testName);
        extentTestThreadLocal.set(test);
        log.debug("ExtentTest node created: {}", testName);
    }

    /**
     * Returns the ExtentTest node for the current thread.
     * Use this in Page classes and test methods to add steps:
     *   ExtentReportManager.getTest().pass("Login button clicked");
     *   ExtentReportManager.getTest().info("Navigating to dashboard");
     *   ExtentReportManager.getTest().fail("Error message not displayed");
     */
    public static ExtentTest getTest() {
        return extentTestThreadLocal.get();
    }

    /**
     * Writes the HTML report file to disk and cleans up ThreadLocal.
     * Must be called in @AfterSuite — NEVER skip this or your report will be empty.
     *
     * Without flush():  the .html file exists but has no test data
     * With flush():     all test nodes, screenshots, and stats are written
     */
    public static synchronized void flushReports() {
        if (extentReports != null) {
            extentReports.flush();
            log.info("ExtentReports flushed — report saved to disk");
        }
        extentTestThreadLocal.remove();
    }

    private static String getTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                .format(new java.util.Date());
    }
}
