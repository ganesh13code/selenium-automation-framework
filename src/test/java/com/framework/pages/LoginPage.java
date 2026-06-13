package com.framework.pages;

import com.aventstack.extentreports.Status;
import com.framework.utils.ExtentReportManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * LoginPage — Page Object for the Login page.
 * Target: https://the-internet.herokuapp.com/login
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: @FindBy annotation
 * ═══════════════════════════════════════════════════════════
 * @FindBy replaces driver.findElement() with a cleaner declarative style.
 *
 * Before PageFactory:
 *   WebElement usernameField = driver.findElement(By.id("username"));
 *   usernameField.sendKeys("admin");  // may throw StaleElementException on reuse
 *
 * With PageFactory @FindBy:
 *   @FindBy(id = "username")
 *   private WebElement usernameField;
 *   // usernameField is re-located every time it's accessed → no StaleElement
 *
 * Locator strategy comparison:
 *   @FindBy(id = "username")              → Fastest, most reliable
 *   @FindBy(name = "username")            → Good for form elements
 *   @FindBy(css = "#login-form .username") → Flexible, readable
 *   @FindBy(xpath = "//input[@id='user']") → Powerful but verbose; last resort
 *   @FindBy(linkText = "Forgot Password")  → Only for anchor text
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Method Chaining (Fluent Interface)
 * ═══════════════════════════════════════════════════════════
 * Action methods return `this` (the current page object), enabling:
 *
 *   loginPage.enterUsername("admin")
 *            .enterPassword("pass")
 *            .clickLogin();
 *
 * vs the verbose alternative:
 *   loginPage.enterUsername("admin");
 *   loginPage.enterPassword("pass");
 *   loginPage.clickLogin();
 *
 * When an action navigates to a new page, return the NEW page object:
 *   public DashboardPage clickLogin() {
 *       click(loginButton);
 *       return new DashboardPage(driver);  // test gets back DashboardPage
 *   }
 * ═══════════════════════════════════════════════════════════
 */
public class LoginPage extends BasePage {

    // ─────────────────────────────────────────────────────
    // LOCATORS — all element references live here
    // ─────────────────────────────────────────────────────

    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(css = "button[type='submit']")
    private WebElement loginButton;

    // The success flash message shown after valid login
    @FindBy(css = ".flash.success")
    private WebElement successMessage;

    // The error flash message shown for invalid credentials
    @FindBy(css = ".flash.error")
    private WebElement errorMessage;

    // ─────────────────────────────────────────────────────
    // CONSTRUCTOR
    // ─────────────────────────────────────────────────────

    /**
     * super(driver) calls BasePage constructor which:
     *   - Stores driver reference
     *   - Creates WaitUtils
     *   - Calls PageFactory.initElements → wires all @FindBy fields above
     */
    public LoginPage(WebDriver driver) {
        super(driver);
        log.info("LoginPage initialized");
    }

    // ─────────────────────────────────────────────────────
    // ACTIONS — public methods tests call
    // ─────────────────────────────────────────────────────

    /** Enters text in the username field. Returns `this` for chaining. */
    public LoginPage enterUsername(String username) {
        type(usernameField, username);
        ExtentReportManager.getTest().log(Status.INFO, "Username entered: " + username);
        return this;
    }

    /** Enters text in the password field. Returns `this` for chaining. */
    public LoginPage enterPassword(String password) {
        type(passwordField, password);
        ExtentReportManager.getTest().log(Status.INFO, "Password entered");
        return this;
    }

    /**
     * Clicks the Login button.
     *
     * NOTE: In a complete framework, a successful login would navigate to a
     * new page, so this would return new DashboardPage(driver).
     * For this example we return LoginPage to stay simple.
     */
    public LoginPage clickLogin() {
        click(loginButton);
        ExtentReportManager.getTest().log(Status.INFO, "Login button clicked");
        return this;
    }

    /**
     * Convenience method: fill credentials and submit in one call.
     * Used by tests that don't need to assert intermediate steps.
     */
    public LoginPage login(String username, String password) {
        return enterUsername(username)
                .enterPassword(password)
                .clickLogin();
    }

    // ─────────────────────────────────────────────────────
    // VERIFICATIONS — methods that check page state
    // ─────────────────────────────────────────────────────

    public boolean isSuccessMessageDisplayed() {
        return isDisplayed(successMessage);
    }

    public String getSuccessMessageText() {
        return getText(successMessage);
    }

    public boolean isErrorMessageDisplayed() {
        return isDisplayed(errorMessage);
    }

    public String getErrorMessageText() {
        return getText(errorMessage);
    }

    /** Returns true if the current URL contains "login" */
    public boolean isOnLoginPage() {
        return getCurrentUrl().contains("login");
    }

    /** Returns true if the current URL indicates a successful login redirect */
    public boolean isLoginSuccessful() {
        // After a valid login on the-internet.herokuapp.com, URL changes to /secure
        waitUtils.waitForUrlContains("secure");
        return getCurrentUrl().contains("secure");
    }
}
