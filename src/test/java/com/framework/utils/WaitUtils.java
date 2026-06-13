package com.framework.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * WaitUtils — Centralized wait strategies for stable element interactions.
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Types of Waits in Selenium (in order of preference)
 * ═══════════════════════════════════════════════════════════
 *
 * 1. EXPLICIT WAIT ✅ (recommended — use this most often)
 *    Waits for a SPECIFIC condition before proceeding.
 *    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
 *    wait.until(ExpectedConditions.visibilityOf(element));
 *    → Polls every 500ms until condition is true, or times out.
 *    → Precise: you choose exactly WHAT you're waiting for.
 *
 * 2. FLUENT WAIT ✅ (explicit wait with more control)
 *    Like explicit wait but you also control:
 *      - Polling interval (how often to check)
 *      - Which exceptions to ignore (e.g. ignore NoSuchElementException)
 *    Best for elements that flicker (appear, disappear, reappear).
 *
 * 3. IMPLICIT WAIT ⚠️  (use sparingly)
 *    A global "wait a bit for every findElement call".
 *    driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
 *    → Set once in BaseTest. Applies to ALL findElement calls automatically.
 *    → Drawback: mixes badly with explicit waits; can cause unexpected delays.
 *
 * 4. Thread.sleep() ❌ NEVER USE
 *    Stops the thread for a FIXED time regardless of page state.
 *    Thread.sleep(3000) → wastes 3 seconds even if element loads in 300ms.
 *    Makes tests slow, brittle, and non-deterministic.
 * ═══════════════════════════════════════════════════════════
 */
public class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);

    private final WebDriverWait wait;
    private final WebDriver driver;
    private final int timeoutSeconds;

    /** Uses explicit.wait from config.properties */
    public WaitUtils(WebDriver driver) {
        this.driver = driver;
        this.timeoutSeconds = ConfigReader.getExplicitWait();
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(this.timeoutSeconds));
    }

    /** Override with a custom timeout for a specific interaction */
    public WaitUtils(WebDriver driver, int customTimeoutSeconds) {
        this.driver = driver;
        this.timeoutSeconds = customTimeoutSeconds;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(customTimeoutSeconds));
    }

    // ─────────────────────────────────────────────────────
    // Visibility waits
    // Use before: reading text, asserting presence, taking screenshots
    // ─────────────────────────────────────────────────────

    /**
     * Waits until the element is present in DOM and visible on screen.
     * "Visible" = not hidden by CSS (display:none, visibility:hidden, opacity:0).
     */
    public WebElement waitForVisibility(WebElement element) {
        log.debug("Waiting {}s for element visibility", timeoutSeconds);
        return wait.until(ExpectedConditions.visibilityOf(element));
    }

    /** Overload: wait using a By locator instead of a WebElement */
    public WebElement waitForVisibility(By locator) {
        log.debug("Waiting {}s for locator to be visible: {}", timeoutSeconds, locator);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    // ─────────────────────────────────────────────────────
    // Clickability waits
    // Use before: clicking buttons, links, checkboxes, dropdowns
    // ─────────────────────────────────────────────────────

    /**
     * Waits until the element is visible AND enabled (not disabled).
     * A button can be visible but disabled — this waits for it to become interactive.
     */
    public WebElement waitForClickability(WebElement element) {
        log.debug("Waiting {}s for element to be clickable", timeoutSeconds);
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    public WebElement waitForClickability(By locator) {
        log.debug("Waiting {}s for locator to be clickable: {}", timeoutSeconds, locator);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // ─────────────────────────────────────────────────────
    // Invisibility waits
    // Use after: clicking submit (wait for loader to disappear),
    //            modal close, toast notification fade out
    // ─────────────────────────────────────────────────────

    /** Waits until element is hidden or removed from DOM */
    public boolean waitForInvisibility(WebElement element) {
        log.debug("Waiting {}s for element to become invisible", timeoutSeconds);
        return wait.until(ExpectedConditions.invisibilityOf(element));
    }

    public boolean waitForInvisibility(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ─────────────────────────────────────────────────────
    // Navigation waits
    // Use after: clicking links, form submissions, redirects
    // ─────────────────────────────────────────────────────

    /** Waits until the page title contains the given text */
    public boolean waitForTitleContains(String titleFragment) {
        log.debug("Waiting for title to contain: '{}'", titleFragment);
        return wait.until(ExpectedConditions.titleContains(titleFragment));
    }

    /** Waits until the current URL contains the given string */
    public boolean waitForUrlContains(String urlFragment) {
        log.debug("Waiting for URL to contain: '{}'", urlFragment);
        return wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    /** Waits until an alert/confirm dialog appears */
    public void waitForAlert() {
        log.debug("Waiting for alert dialog");
        wait.until(ExpectedConditions.alertIsPresent());
    }

    // ─────────────────────────────────────────────────────
    // Fluent Wait
    // Use for: elements that load asynchronously and may flicker
    // ─────────────────────────────────────────────────────

    /**
     * Fluent wait: polls every 1 second, ignores NoSuchElementException.
     *
     * WHEN TO USE FLUENT WAIT:
     * A progress bar that shows/hides, a lazy-loaded list, a live search
     * that returns results after a delay — elements that may not be in DOM yet
     * and might throw exceptions during polling.
     *
     * @param locator        element to wait for
     * @param timeoutSeconds max wait time
     * @param pollEveryMs    how often to check (in milliseconds)
     */
    public WebElement fluentWait(By locator, int timeoutSeconds, int pollEveryMs) {
        FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(Duration.ofMillis(pollEveryMs))
                .ignoring(org.openqa.selenium.NoSuchElementException.class)
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class);

        log.debug("Fluent wait: timeout={}s, poll={}ms, locator={}", timeoutSeconds, pollEveryMs, locator);
        return fluentWait.until(d -> d.findElement(locator));
    }
}
