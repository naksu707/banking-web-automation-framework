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

    /** Balance shown for one account, as text (for example "$515.50"). Waits for the AJAX table to load. */
    public String balanceText(String accountNumber) {
        accountLink(accountNumber).waitFor();
        return accountsTable().locator("tbody tr:has(a:text-is('" + accountNumber + "')) td").nth(1).innerText().trim();
    }
}
