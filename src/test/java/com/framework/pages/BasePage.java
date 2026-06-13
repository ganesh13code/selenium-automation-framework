package com.framework.pages;

import com.aventstack.extentreports.Status;
import com.framework.utils.ExtentReportManager;
import com.framework.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.Select;

/**
 * BasePage — The parent class for all Page Object classes.
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Page Object Model (POM)
 * ═══════════════════════════════════════════════════════════
 * POM separates WHAT a page contains (locators) from HOW tests use it.
 *
 * WITHOUT POM — locators scattered in test code:
 *   // In LoginTest.java:
 *   driver.findElement(By.id("user")).sendKeys("admin");
 *   driver.findElement(By.id("pass")).sendKeys("pass");
 *   driver.findElement(By.id("btn")).click();
 *   // Same 3 lines repeated in 10 test files
 *   // When the "user" id changes → fix 10 files
 *
 * WITH POM — locators live in one page class:
 *   // In LoginPage.java:
 *   @FindBy(id = "user") WebElement usernameField;
 *   public void login(String u, String p) { ... }
 *
 *   // In LoginTest.java:
 *   loginPage.login("admin", "pass");
 *   // When "user" id changes → fix only LoginPage.java
 *
 * Benefits:
 *   ✅ Maintainability: one place per locator
 *   ✅ Readability: test code reads like English
 *   ✅ Reusability: login() called from 10 tests, defined once
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: PageFactory
 * ═══════════════════════════════════════════════════════════
 * PageFactory.initElements(driver, this) processes @FindBy annotations
 * and replaces each field with a dynamic proxy (AjaxElementLocator).
 *
 * Key behavior — LAZY initialization:
 *   - Elements are NOT found in DOM when the page class is constructed
 *   - They are found the FIRST TIME each field is accessed in code
 *   - They are RE-FOUND on every access, which handles StaleElementException
 *
 * @FindBy locator priority (fastest → slowest):
 *   id="..."         → #1 Fastest — direct DOM lookup
 *   name="..."       → Good for form inputs
 *   css="..."        → Flexible, readable, cross-browser
 *   xpath="..."      → Most powerful but slowest; use as last resort
 *   linkText="..."   → Only for <a> tags with exact visible text
 *   className="..."  → Fine for single classes, not compound selectors
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Why BasePage?
 * ═══════════════════════════════════════════════════════════
 * Every page needs: click, type, wait, getText, isDisplayed, scroll...
 * Instead of copying these methods into every page class (30+ pages),
 * define them ONCE in BasePage and inherit everywhere.
 *
 * LoginPage extends BasePage → gets all actions for free
 * CartPage  extends BasePage → gets all actions for free
 * ═══════════════════════════════════════════════════════════
 */
public class BasePage {

    protected final WebDriver driver;
    protected final WaitUtils waitUtils;
    protected static final Logger log = LogManager.getLogger(BasePage.class);

    /**
     * Every page class calls super(driver) which:
     * 1. Stores the driver reference
     * 2. Creates a WaitUtils instance
     * 3. Wires up all @FindBy annotations via PageFactory
     */
    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver);
        PageFactory.initElements(driver, this); // wire @FindBy fields
    }

    // ─────────────────────────────────────────────────────
    // Core Actions
    // ─────────────────────────────────────────────────────

    /**
     * Waits for clickability, then clicks the element.
     * Logs the action to the ExtentReport.
     */
    protected void click(WebElement element) {
        waitUtils.waitForClickability(element);
        element.click();
        log.debug("Clicked element");
        logStep("Clicked element");
    }

    /**
     * Clears the field, then types the given text.
     * Waits for visibility before typing.
     */
    protected void type(WebElement element, String text) {
        waitUtils.waitForVisibility(element);
        element.clear();
        element.sendKeys(text);
        log.debug("Typed: '{}'", text);
        logStep("Typed: '" + text + "'");
    }

    /**
     * Selects a native <select> dropdown by visible text.
     * NOTE: This only works on <select> HTML elements.
     * For custom React/Material UI dropdowns, use click() on the option.
     */
    protected void selectByVisibleText(WebElement dropdown, String text) {
        waitUtils.waitForVisibility(dropdown);
        new Select(dropdown).selectByVisibleText(text);
        log.debug("Selected '{}' from dropdown", text);
        logStep("Selected: '" + text + "'");
    }

    protected void selectByIndex(WebElement dropdown, int index) {
        waitUtils.waitForVisibility(dropdown);
        new Select(dropdown).selectByIndex(index);
    }

    protected void selectByValue(WebElement dropdown, String value) {
        waitUtils.waitForVisibility(dropdown);
        new Select(dropdown).selectByValue(value);
    }

    // ─────────────────────────────────────────────────────
    // JavaScript Actions
    // Use when regular Selenium interactions are blocked by overlays
    // ─────────────────────────────────────────────────────

    /**
     * Scrolls the element into the visible viewport.
     * Required when clicking elements below the fold.
     */
    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", element);
        log.debug("Scrolled element into view");
    }

    /**
     * Clicks via JavaScript executor.
     * Use when a regular click throws ElementClickInterceptedException
     * (e.g., a cookie banner or tooltip is covering the element).
     */
    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        log.debug("JS-clicked element");
        logStep("JS clicked element");
    }

    /**
     * Sets the value of an input field directly via JavaScript.
     * Use for file inputs, date pickers, or readonly fields.
     */
    protected void jsSetValue(WebElement element, String value) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].value = arguments[1];", element, value);
        log.debug("JS set value: '{}'", value);
    }

    // ─────────────────────────────────────────────────────
    // Getters / Verifications
    // ─────────────────────────────────────────────────────

    /** Returns visible text of element, trimmed of whitespace */
    protected String getText(WebElement element) {
        waitUtils.waitForVisibility(element);
        return element.getText().trim();
    }

    protected String getAttribute(WebElement element, String attribute) {
        return element.getAttribute(attribute);
    }

    /**
     * Safe isDisplayed — returns false instead of throwing an exception.
     * Selenium's element.isDisplayed() throws NoSuchElementException
     * if the element is not in DOM at all. This wraps it safely.
     */
    protected boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    protected boolean isEnabled(WebElement element) {
        try {
            return element.isEnabled();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /** Returns the current page <title> */
    protected String getPageTitle() {
        return driver.getTitle();
    }

    /** Returns the full URL of the current page */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    // ─────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────

    /** Safely logs a step to ExtentReport — silently ignores if report not initialized */
    private void logStep(String message) {
        try {
            if (ExtentReportManager.getTest() != null) {
                ExtentReportManager.getTest().log(Status.INFO, message);
            }
        } catch (Exception ignored) {
            // Don't let reporting errors break test execution
        }
    }
}
