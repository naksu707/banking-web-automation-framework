package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/** ParaBank home page with the "Customer Login" form. */
public class HomePage extends BasePage {

    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator loginButton;

    public HomePage(Page page) {
        super(page);
        this.usernameInput = page.locator("#loginPanel input[name='username']");
        this.passwordInput = page.locator("#loginPanel input[name='password']");
        this.loginButton = page.locator("#loginPanel input[value='Log In']");
    }

    @Step("Open the ParaBank home page")
    public HomePage open() {
        page.navigate("index.htm");
        return this;
    }

    @Step("Log in as {username}")
    public AccountsOverviewPage loginAs(String username, String password) {
        submitLogin(username, password);
        return new AccountsOverviewPage(page);
    }

    /** Submits the form and stays on the result page, for negative scenarios. */
    @Step("Try to log in as \"{username}\"")
    public HomePage attemptLogin(String username, String password) {
        submitLogin(username, password);
        return this;
    }

    public Locator loginForm() {
        return page.locator("#loginPanel");
    }

    public Locator errorMessage() {
        return page.locator("#rightPanel p.error");
    }

    private void submitLogin(String username, String password) {
        usernameInput.fill(username);
        passwordInput.fill(password);
        loginButton.click();
    }
}
