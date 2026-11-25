package parabank.tests.ui;

import parabank.utils.BaseTest;
import parabank.data.Customer;
import parabank.data.TestDataFactory;
import parabank.pages.RegisterPage;
import parabank.data.TestUsers;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static parabank.pages.RegisterPage.*;
import static org.junit.jupiter.api.Assertions.assertAll;

@Epic("Authentication")
@Feature("Registration")
@DisplayName("Registration")
class RegistrationTest extends BaseTest {

    @Test
    @Tag("smoke")
    @Story("Successful registration")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("A new customer can register and is logged in automatically")
    void newCustomerCanRegister() {
        Customer customer = TestDataFactory.newCustomer();

        RegisterPage result = new RegisterPage(page).open().register(customer);

        assertThat(result.title()).hasText("Welcome " + customer.username());
        assertThat(result.successMessage())
                .hasText("Your account was created successfully. You are now logged in.");
        assertThat(result.welcomeMessage()).containsText(customer.fullName());
    }

    @Test
    @Story("Validation")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("A username that already exists cannot be registered again")
    void duplicateUsernameIsRejected() {
        Customer existing = TestUsers.registerNewCustomer();
        Customer newcomer = TestDataFactory.newCustomer().withUsername(existing.username());

        RegisterPage result = new RegisterPage(page).open().register(newcomer);

        assertThat(result.fieldError(USERNAME)).hasText("This username already exists.");
        assertThat(result.welcomeMessage()).isHidden();
    }

    @Test
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Registration fails when the password confirmation does not match")
    void passwordConfirmationMustMatch() {
        Customer customer = TestDataFactory.newCustomer();

        RegisterPage result = new RegisterPage(page).open().register(customer, customer.password() + "x");

        assertThat(result.fieldError(CONFIRM_PASSWORD)).hasText("Passwords did not match.");
        assertThat(result.welcomeMessage()).isHidden();
    }

    @Test
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Submitting an empty form shows a message for every required field")
    void requiredFieldsAreValidated() {
        RegisterPage form = new RegisterPage(page).open().submit();

        // assertAll reports every missing message at once instead of stopping at the first one
        assertAll(
                () -> assertThat(form.fieldError(FIRST_NAME)).hasText("First name is required."),
                () -> assertThat(form.fieldError(LAST_NAME)).hasText("Last name is required."),
                () -> assertThat(form.fieldError(STREET)).hasText("Address is required."),
                () -> assertThat(form.fieldError(CITY)).hasText("City is required."),
                () -> assertThat(form.fieldError(STATE)).hasText("State is required."),
                () -> assertThat(form.fieldError(ZIP_CODE)).hasText("Zip Code is required."),
                () -> assertThat(form.fieldError(SSN)).hasText("Social Security Number is required."),
                () -> assertThat(form.fieldError(USERNAME)).hasText("Username is required."),
                () -> assertThat(form.fieldError(PASSWORD)).hasText("Password is required."),
                () -> assertThat(form.fieldError(CONFIRM_PASSWORD)).hasText("Password confirmation is required."));
    }
}
