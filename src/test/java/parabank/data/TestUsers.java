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

    /** Attempts before giving up: the public ParaBank demo sometimes fails a request. */
    private static final int MAX_ATTEMPTS = 3;

    @Step("Precondition: register a new customer")
    public static Customer registerNewCustomer() {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Customer customer = TestDataFactory.newCustomer();
            try {
                register(customer);
                Logs.info("Test customer registered: {}", customer.username());
                return customer;
            } catch (RuntimeException e) {
                lastError = e;
                Logs.warn("Registration attempt {}/{} failed for {}: {}", attempt, MAX_ATTEMPTS,
                        customer.username(), firstLine(e.getMessage()));
            }
        }
        throw new IllegalStateException("Could not register a test customer after " + MAX_ATTEMPTS
                + " attempts. ParaBank may be unavailable.", lastError);
    }

    private static void register(Customer customer) {
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
                throw new IllegalStateException("Unexpected page title after registering: \"" + actualTitle + "\"");
            }
        }
    }

    private static String firstLine(String message) {
        return message == null ? "" : message.lines().findFirst().orElse("");
    }

    private static BrowserType browserType(Playwright playwright) {
        return switch (Config.browser()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };
    }
}
