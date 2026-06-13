package com.framework.utils;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ScreenshotUtils — Captures screenshots for failed tests.
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Why capture screenshots on failure?
 * ═══════════════════════════════════════════════════════════
 * A failed assertion gives you a message. A screenshot shows you:
 *   - An unexpected popup that blocked the click
 *   - A UI element that changed its position or style
 *   - A broken page that loaded a 404
 *   - An environment issue (login page instead of dashboard)
 *
 * Screenshots are the #1 debugging tool in UI automation.
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: OutputType.FILE vs OutputType.BASE64
 * ═══════════════════════════════════════════════════════════
 * OutputType.FILE    → saves screenshot as a temp .png file on disk
 *                       we then copy it to our desired location
 *
 * OutputType.BASE64  → screenshot as a Base64-encoded ASCII string
 *                       used to EMBED directly in HTML reports:
 *                       <img src="data:image/png;base64,iVBORw0KGgo..."/>
 *                       The HTML report is self-contained — no external files needed.
 *
 * This framework does BOTH:
 *   - Saves the .png to disk (useful for CI artifact uploads)
 *   - Embeds Base64 in the ExtentReport HTML (for stakeholder sharing)
 * ═══════════════════════════════════════════════════════════
 */
public class ScreenshotUtils {

    private static final Logger log = LogManager.getLogger(ScreenshotUtils.class);

    private ScreenshotUtils() {}

    /**
     * Captures a screenshot and saves it as a .png file to disk.
     *
     * @param driver   the active WebDriver for the current thread
     * @param testName used in the filename so you can identify which test failed
     * @return absolute path of the saved screenshot, or null if capture fails
     *
     * Output path example:
     *   test-output/screenshots/testInvalidLogin_20240315_143022.png
     */
    public static String captureScreenshot(WebDriver driver, String testName) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String dir = ConfigReader.getScreenshotDir();
        String filePath = dir + File.separator + testName + "_" + timestamp + ".png";

        try {
            // Ensure output directory exists
            File outputDir = new File(dir);
            if (!outputDir.exists()) outputDir.mkdirs();

            // TakesScreenshot is the Selenium interface for screenshot capture
            // All major WebDriver implementations support it (Chrome, Firefox, Edge)
            File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(srcFile, new File(filePath));
            log.info("Screenshot saved: {}", filePath);
            return filePath;

        } catch (IOException e) {
            log.error("Failed to save screenshot to disk: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Screenshot capture failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Captures a screenshot and returns it as a Base64 string.
     * Pass this directly to ExtentReports to embed in the HTML report.
     *
     * Usage in BaseTest teardown:
     *   String base64 = ScreenshotUtils.captureScreenshotAsBase64(driver);
     *   extentTest.fail("Test failed",
     *       MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
     *
     * @return Base64-encoded PNG string, or null if capture fails
     */
    public static String captureScreenshotAsBase64(WebDriver driver) {
        try {
            return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            log.error("Failed to capture Base64 screenshot: {}", e.getMessage());
            return null;
        }
    }
}
