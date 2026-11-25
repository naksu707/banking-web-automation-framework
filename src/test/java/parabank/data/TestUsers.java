package parabank.data;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.Step;
import parabank.pages.RegisterPage;
import parabank.utils.Config;
import parabank.utils.Logs;

/**
 * Registers fresh customers for the tests.
 * ParaBank's API has no registration endpoint, so this goes through the UI,
 * with its own short-lived Playwright instance so it can run in @BeforeAll.
 */
public final class TestUsers {

    private TestUsers() {
    }

    @Step("Precondition: register a new customer")
    public static Customer registerNewCustomer() {
        Customer customer = TestDataFactory.newCustomer();

        try (Playwright playwright = Playwright.create();
             Browser browser = browserType(playwright).launch(new BrowserType.LaunchOptions().setHeadless(Config.headless()));
             BrowserContext context = browser.newContext(new Browser.NewContextOptions().setBaseURL(Config.baseUrl()))) {

            context.setDefaultTimeout(Config.timeout());
            Page page = context.newPage();
            RegisterPage registerPage = new RegisterPage(page).open().register(customer);

            String expectedTitle = "Welcome " + customer.username();
            registerPage.title().waitFor();
            String actualTitle = registerPage.title().textContent().trim();
            if (!actualTitle.equals(expectedTitle)) {
                throw new IllegalStateException("Could not register test customer " + customer.username()
                        + ". Page title was: \"" + actualTitle + "\". ParaBank may be unavailable.");
            }
        }

        Logs.info("Test customer registered: {}", customer.username());
        return customer;
    }

    private static BrowserType browserType(Playwright playwright) {
        return switch (Config.browser()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
    }
}
