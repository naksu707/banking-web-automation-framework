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

    /** Link of one specific account in the overview table. */
    public Locator accountLink(String accountNumber) {
        return accountsTable().locator("tbody a:text-is('" + accountNumber + "')");
    }
}
