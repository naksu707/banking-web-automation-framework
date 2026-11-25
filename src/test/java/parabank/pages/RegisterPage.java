package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import parabank.data.Customer;
import io.qameta.allure.Step;

/** "Signing up is easy!" registration form. */
public class RegisterPage extends BasePage {

    /** Field ids used by ParaBank. They contain dots, so they are matched with [id='...']. */
    public static final String FIRST_NAME = "customer.firstName";
    public static final String LAST_NAME = "customer.lastName";
    public static final String STREET = "customer.address.street";
    public static final String CITY = "customer.address.city";
    public static final String STATE = "customer.address.state";
    public static final String ZIP_CODE = "customer.address.zipCode";
    public static final String PHONE = "customer.phoneNumber";
    public static final String SSN = "customer.ssn";
    public static final String USERNAME = "customer.username";
    public static final String PASSWORD = "customer.password";
    public static final String CONFIRM_PASSWORD = "repeatedPassword";

    public RegisterPage(Page page) {
        super(page);
    }

    @Step("Open the registration page")
    public RegisterPage open() {
        page.navigate("register.htm");
        return this;
    }

    @Step("Register customer {customer.username}")
    public RegisterPage register(Customer customer) {
        return register(customer, customer.password());
    }

    /** Lets negative tests send a confirmation that differs from the password. */
    @Step("Register customer {customer.username} with a custom password confirmation")
    public RegisterPage register(Customer customer, String confirmPassword) {
        field(FIRST_NAME).fill(customer.firstName());
        field(LAST_NAME).fill(customer.lastName());
        field(STREET).fill(customer.street());
        field(CITY).fill(customer.city());
        field(STATE).fill(customer.state());
        field(ZIP_CODE).fill(customer.zipCode());
        field(PHONE).fill(customer.phoneNumber());
        field(SSN).fill(customer.ssn());
        field(USERNAME).fill(customer.username());
        field(PASSWORD).fill(customer.password());
        field(CONFIRM_PASSWORD).fill(confirmPassword);
        return submit();
    }

    @Step("Submit the registration form")
    public RegisterPage submit() {
        page.locator("input[value='Register']").click();
        return this;
    }

    /** Confirmation text shown after a successful registration. */
    public Locator successMessage() {
        return page.locator("#rightPanel p").first();
    }

    /** Validation message shown next to a field, e.g. fieldError(USERNAME). */
    public Locator fieldError(String fieldId) {
        return page.locator("[id='" + fieldId + ".errors']");
    }

    private Locator field(String fieldId) {
        return page.locator("[id='" + fieldId + "']");
    }
}
