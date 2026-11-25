package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** "Account Details" page (activity.htm?id=...). The details are loaded with AJAX. */
public class AccountActivityPage extends BasePage {

    public AccountActivityPage(Page page) {
        super(page);
    }

    public Locator accountId() {
        return page.locator("#accountId");
    }

    public Locator accountType() {
        return page.locator("#accountType");
    }

    public Locator balance() {
        return page.locator("#balance");
    }
}
