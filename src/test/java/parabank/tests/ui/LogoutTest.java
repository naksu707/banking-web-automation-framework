package parabank.tests.ui;

import parabank.utils.BaseTest;
import parabank.data.Customer;
import parabank.pages.HomePage;
import parabank.data.TestUsers;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Epic("Authentication")
@Feature("Logout")
@DisplayName("Logout")
class LogoutTest extends BaseTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Logging out ends the session and shows the login form again")
    void logoutReturnsToLoginForm() {
        HomePage home = new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .logOut();

        assertThat(page).hasURL(Pattern.compile(".*index\\.htm.*"));
        assertThat(home.loginForm()).isVisible();
        assertThat(home.welcomeMessage()).isHidden();
    }
}
