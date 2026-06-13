package com.framework.utils;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

/**
 * DriverManager — Creates, stores, and destroys WebDriver instances.
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: ThreadLocal<T>
 * ═══════════════════════════════════════════════════════════
 * ThreadLocal creates a SEPARATE copy of a variable for EACH thread.
 *
 * Imagine 3 test methods running in parallel:
 *   Thread-1 runs testValidLogin()
 *   Thread-2 runs testInvalidLogin()
 *   Thread-3 runs testEmptyPassword()
 *
 * WITHOUT ThreadLocal (shared static driver):
 *   Thread-1 opens Chrome #1
 *   Thread-2 accidentally uses Chrome #1 → tests crash into each other
 *
 * WITH ThreadLocal:
 *   Thread-1 has its own Chrome #1
 *   Thread-2 has its own Chrome #2
 *   Thread-3 has its own Chrome #3
 *   Each thread is completely isolated — no interference.
 *
 * This is the KEY to thread-safe parallel test execution.
 * ═══════════════════════════════════════════════════════════
 */
public class DriverManager {

    private static final Logger log = LogManager.getLogger(DriverManager.class);

    /**
     * ThreadLocal stores one WebDriver per thread.
     * driverThreadLocal.get() always returns the driver for the CURRENT thread.
     * driverThreadLocal.set(driver) sets the driver for the CURRENT thread.
     */
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    // Utility class — prevent instantiation
    private DriverManager() {}

    /**
     * Get the WebDriver for the current running thread.
     * Call this from anywhere: DriverManager.getDriver()
     */
    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /**
     * Initialize and store a new WebDriver for the current thread.
     * Called once per test in @BeforeMethod.
     *
     * @param browser  "chrome" | "firefox" | "edge"
     * @param headless true = no visible browser window (for CI pipelines)
     */
    public static void initDriver(String browser, boolean headless) {
        WebDriver driver;

        switch (browser.toLowerCase().trim()) {

            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions ffOptions = new FirefoxOptions();
                if (headless) ffOptions.addArguments("-headless");
                driver = new FirefoxDriver(ffOptions);
                log.info("Firefox launched [headless={}]", headless);
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                driver = new EdgeDriver();
                log.info("Edge launched");
                break;

            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    // '--headless=new' is the modern headless flag in Chrome 112+
                    options.addArguments("--headless=new");
                }
                options.addArguments("--start-maximized");
                options.addArguments("--disable-notifications");
                options.addArguments("--no-sandbox");           // required in Docker/CI
                options.addArguments("--disable-dev-shm-usage"); // prevents crashes in Docker
                driver = new ChromeDriver(options);
                log.info("Chrome launched [headless={}]", headless);
                break;
        }

        // Store the driver in ThreadLocal for THIS thread
        driverThreadLocal.set(driver);
    }

    /**
     * Quit the browser AND remove the ThreadLocal reference.
     * Called in @AfterMethod after every test.
     *
     * ═══════════════════════════════════════════════════
     * WHY call remove() AND quit()?
     * ═══════════════════════════════════════════════════
     * driver.quit()              → closes the browser window (OS process ends)
     * driverThreadLocal.remove() → removes the Java reference from thread memory
     *
     * Skipping remove() causes a memory leak in thread pools:
     * The thread is reused for the next test but still holds a stale,
     * closed WebDriver reference. Calling getDriver() returns a dead driver.
     * ═══════════════════════════════════════════════════
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove(); // CRITICAL: clean up ThreadLocal
            log.info("Browser closed. ThreadLocal WebDriver removed.");
        }
    }
}
