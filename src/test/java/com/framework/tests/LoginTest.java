package com.framework.tests;

import com.aventstack.extentreports.Status;
import com.framework.base.BaseTest;
import com.framework.pages.LoginPage;
import com.framework.utils.ExtentReportManager;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

/**
 * LoginTest — Tests for the Login page.
 * Extends BaseTest to inherit browser setup/teardown automatically.
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Arrange-Act-Assert (AAA) Pattern
 * ═══════════════════════════════════════════════════════════
 * Every test follows three clear sections:
 *
 *   ARRANGE: Set up what you need (page objects, test data)
 *   ACT:     Perform the action being tested
 *   ASSERT:  Verify the expected outcome
 *
 * This structure makes tests self-documenting and easy to debug.
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: @Test attributes
 * ═══════════════════════════════════════════════════════════
 *   groups       → categorize tests for selective execution
 *                  mvn test -Dgroups=smoke       (only smoke tests)
 *                  mvn test -Dgroups=regression  (all regression tests)
 *
 *   priority     → controls execution order within a class
 *                  lower number = runs earlier (default = 0)
 *
 *   description  → documents what the test verifies
 *                  shows in the ExtentReport
 *
 *   enabled      → false = skip this test entirely
 *
 *   dependsOnMethods → "run this test ONLY IF that test passed"
 *                      use sparingly — creates fragile dependencies
 * ═══════════════════════════════════════════════════════════
 *
 * ═══════════════════════════════════════════════════════════
 * CONCEPT: Hard Assert vs Soft Assert
 * ═══════════════════════════════════════════════════════════
 * Assert (Hard Assert):
 *   Assert.assertTrue(condition);
 *   → Test STOPS immediately on first failure
 *   → Use when subsequent assertions make no sense if this one fails
 *
 * SoftAssert:
 *   SoftAssert sa = new SoftAssert();
 *   sa.assertTrue(condition1);
 *   sa.assertEquals(actual, expected);
 *   sa.assertAll();  // reports ALL failures together at the end
 *   → Test CONTINUES even when an assertion fails
 *   → Use when you want to check multiple things in one test run
 * ═══════════════════════════════════════════════════════════
 */
public class LoginTest extends BaseTest {

    // The real credentials for the-internet.herokuapp.com
    private static final String VALID_USERNAME = "tomsmith";
    private static final String VALID_PASSWORD = "SuperSecretPassword!";

    // ─────────────────────────────────────────────────────
    // TEST METHODS
    // ─────────────────────────────────────────────────────

    @Test(
        groups       = { "smoke", "regression" },
        priority     = 1,
        description  = "Verify valid credentials produce a successful login"
    )
    public void testValidLogin() {
        // ARRANGE
        ExtentReportManager.getTest().log(Status.INFO, "Scenario: Valid login with correct credentials");
        LoginPage loginPage = new LoginPage(getDriver());

        // ACT
        loginPage.login(VALID_USERNAME, VALID_PASSWORD);

        // ASSERT
        ExtentReportManager.getTest().log(Status.INFO, "Asserting: success message visible");
        Assert.assertTrue(
            loginPage.isSuccessMessageDisplayed(),
            "Success message should appear after valid login"
        );
        Assert.assertFalse(
            loginPage.isErrorMessageDisplayed(),
            "Error message should NOT appear after valid login"
        );
        ExtentReportManager.getTest().pass("Valid login verified ✓");
    }

    @Test(
        groups      = { "regression" },
        priority    = 2,
        description = "Verify invalid credentials display an error message"
    )
    public void testInvalidLogin() {
        // ARRANGE
        LoginPage loginPage = new LoginPage(getDriver());

        // ACT
        loginPage.login("invalidUser", "wrongPassword");

        // ASSERT — using SoftAssert to check both message presence AND text
        SoftAssert sa = new SoftAssert();
        sa.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error message should be displayed");
        sa.assertTrue(loginPage.getErrorMessageText().contains("invalid"),
                "Error text should mention 'invalid'");
        sa.assertAll(); // reports all failures at once

        ExtentReportManager.getTest().pass("Invalid login error verified ✓");
    }

    @Test(
        groups      = { "regression" },
        priority    = 3,
        description = "Verify empty username shows an error"
    )
    public void testLoginWithEmptyUsername() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterPassword(VALID_PASSWORD).clickLogin();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error message should appear when username is empty");
    }

    @Test(
        groups      = { "regression" },
        priority    = 4,
        description = "Verify empty password shows an error"
    )
    public void testLoginWithEmptyPassword() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.enterUsername(VALID_USERNAME).clickLogin();

        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
                "Error message should appear when password is empty");
    }

    @Test(
        groups      = { "regression" },
        priority    = 5,
        description = "Verify the login page title"
    )
    public void testLoginPageTitle() {
        LoginPage loginPage = new LoginPage(getDriver());

        Assert.assertTrue(
            loginPage.isOnLoginPage(),
            "Should be on the login page after navigating to base URL"
        );
    }

    // ─────────────────────────────────────────────────────
    // DATA-DRIVEN TEST
    // ─────────────────────────────────────────────────────

    /**
     * CONCEPT: @DataProvider
     * ═══════════════════════════════════════════════════════
     * Returns a 2D Object[][] — each inner array is one test run.
     * The test method below receives each row as its parameters.
     *
     * This is DATA-DRIVEN testing (the "D" in Hybrid Framework).
     * The test logic is written ONCE; multiple data sets drive it.
     *
     * Columns here: { username, password, expectedContains }
     *   Row 1: blank/blank        → error must contain "username"
     *   Row 2: valid user, wrong  → error must contain "password"
     *   Row 3: wrong/wrong        → error must contain "invalid"
     *
     * Advanced: @DataProvider can also read from Excel files via
     * Apache POI, or from JSON/CSV — making it truly data-driven.
     * ═══════════════════════════════════════════════════════
     */
    @DataProvider(name = "invalidCredentialCombinations")
    public Object[][] invalidCredentialCombinations() {
        return new Object[][] {
            { "",               "",                      "Username" },
            { VALID_USERNAME,   "wrongPassword",         "password" },
            { "unknownUser",    VALID_PASSWORD,          "invalid"  },
            { "unknownUser",    "unknownPass",           "invalid"  },
        };
    }

    @Test(
        dataProvider = "invalidCredentialCombinations",
        groups       = { "regression" },
        priority     = 6,
        description  = "Data-driven: multiple invalid credential combinations"
    )
    public void testInvalidCredentialCombinations(String username, String password, String expectedErrorContains) {
        // ARRANGE
        LoginPage loginPage = new LoginPage(getDriver());
        ExtentReportManager.getTest().log(Status.INFO,
            "Data row → user: '" + username + "' | pass: '" + password + "'");

        // ACT
        loginPage.login(username, password);

        // ASSERT
        Assert.assertTrue(loginPage.isErrorMessageDisplayed(),
            "Error message not shown for: user='" + username + "' pass='" + password + "'");

        String actualError = loginPage.getErrorMessageText();
        Assert.assertTrue(actualError.toLowerCase().contains(expectedErrorContains.toLowerCase()),
            "Expected error containing '" + expectedErrorContains + "' but got: '" + actualError + "'");

        ExtentReportManager.getTest().pass("Row verified ✓ | Error: " + actualError);
    }
}
