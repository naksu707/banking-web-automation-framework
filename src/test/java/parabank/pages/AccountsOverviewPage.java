package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Landing page after a successful login. */
public class AccountsOverviewPage extends BasePage {

    public AccountsOverviewPage(Page page) {
        super(page);
    }

    public Locator accountsTable() {
        return page.locator("#accountTable");
    }

    /** Account number links in the overview table. */
    public Locator accountLinks() {
        return accountsTable().locator("tbody a");
    }
}
