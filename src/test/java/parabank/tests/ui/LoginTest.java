package parabank.tests.ui;

import parabank.utils.BaseTest;
import parabank.data.Customer;
import parabank.pages.AccountsOverviewPage;
import parabank.pages.HomePage;
import parabank.data.TestUsers;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Epic("Authentication")
@Feature("Login")
@DisplayName("Login")
class LoginTest extends BaseTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Tag("smoke")
    @Story("Valid login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A registered customer can log in and sees the accounts overview")
    void validLoginOpensAccountsOverview() {
        AccountsOverviewPage overview = new HomePage(page).open()
                .loginAs(customer.username(), customer.password());

        assertThat(page).hasURL(Pattern.compile(".*overview\\.htm.*"));
        assertThat(overview.title()).hasText("Accounts Overview");
        assertThat(overview.accountsTable()).isVisible();
    }

    @Test
    @Story("Valid login")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("After login, the welcome message shows the customer's full name")
    void welcomeMessageShowsCustomerName() {
        AccountsOverviewPage overview = new HomePage(page).open()
                .loginAs(customer.username(), customer.password());

        assertThat(overview.welcomeMessage()).containsText(customer.fullName());
    }

    static Stream<Arguments> invalidCredentials() {
        return Stream.of(
                Arguments.of("wrong password", "REGISTERED", "WrongPass123"),
                Arguments.of("unknown username", "unknown_user_x9z", "AnyPass123"));
    }

    /** Security requirement: empty credentials must never open the account area. */
    @Test
    @Tag("smoke")
    @Story("Invalid login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Login is rejected with empty credentials")
    void emptyCredentialsAreRejected() {
        HomePage home = new HomePage(page).open().attemptLogin("", "");

        assertLoginRejected(home);
    }

    /**
     * Security requirement: wrong credentials must never open the account area.
     * Known defect BUG-003 (docs/defects.md): ParaBank opens another customer's account
     * ("John Smith") when the credentials are wrong.
     * Excluded from the default run; run it with: mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @ParameterizedTest(name = "BUG-003 · {0}")
    @MethodSource("invalidCredentials")
    @Tag("known-defect")
    @Story("Invalid login")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("BUG-003 · Login is rejected with wrong credentials")
    void invalidLoginIsRejected(String scenario, String username, String password) {
        String user = "REGISTERED".equals(username) ? customer.username() : username;

        HomePage home = new HomePage(page).open().attemptLogin(user, password);

        assertLoginRejected(home);
    }

    /**
     * No session is opened. The title is checked first because it waits for the page that answers
     * the login; checking that something is hidden before that would pass while still on the old page.
     */
    private void assertLoginRejected(HomePage home) {
        assertThat(home.title()).hasText("Error!");
        assertThat(home.welcomeMessage()).isHidden();
        assertThat(page).not().hasURL(Pattern.compile(".*overview\\.htm.*"));
    }

    @Test
    @Story("Invalid login")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Empty credentials show a clear validation message")
    void emptyCredentialsShowValidationMessage() {
        HomePage home = new HomePage(page).open().attemptLogin("", "");

        assertThat(home.errorMessage()).hasText("Please enter a username and password.");
    }

    /**
     * Known defect BUG-001 (docs/defects.md): ParaBank answers wrong credentials with
     * "An internal error has occurred and has been logged." instead of a clear message.
     * Excluded from the default run; run it with: mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCredentials")
    @Tag("known-defect")
    @Story("Invalid login")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("BUG-001 · Wrong credentials show a clear error message")
    void wrongCredentialsShowClearErrorMessage(String scenario, String username, String password) {
        Assumptions.assumeFalse(username.isEmpty(), "Empty credentials are covered by another test");
        String user = "REGISTERED".equals(username) ? customer.username() : username;

        HomePage home = new HomePage(page).open().attemptLogin(user, password);

        assertThat(home.errorMessage()).hasText("The username and password could not be verified.");
    }
}
