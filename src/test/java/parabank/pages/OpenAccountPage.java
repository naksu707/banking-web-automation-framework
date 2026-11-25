package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import parabank.data.AccountType;

/**
 * "Open New Account" page.
 * The list of funding accounts is loaded with AJAX, so the page waits for it before submitting.
 */
public class OpenAccountPage extends BasePage {

    private final Locator accountTypeSelect;
    private final Locator fromAccountSelect;
    private final Locator openAccountButton;

    public OpenAccountPage(Page page) {
        super(page);
        this.accountTypeSelect = page.locator("#type");
        this.fromAccountSelect = page.locator("#fromAccountId");
        this.openAccountButton = page.locator("input[value='Open New Account']");
    }

    @Step("Open the \"Open New Account\" page")
    public OpenAccountPage open() {
        page.navigate("openaccount.htm");
        return this;
    }

    /** Opens an account funded from the first account in the list. */
    @Step("Open a new {type} account")
    public OpenAccountPage openAccount(AccountType type) {
        fromAccountOptions().first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.ATTACHED));
        return submit(type, null);
    }

    @Step("Open a new {type} account funded from account {fromAccountId}")
    public OpenAccountPage openAccount(AccountType type, String fromAccountId) {
        return submit(type, fromAccountId);
    }

    /** Accounts offered as the source of the initial deposit. */
    public Locator fromAccountOptions() {
        return fromAccountSelect.locator("option");
    }

    public Locator resultTitle() {
        return page.locator("#openAccountResult h1.title");
    }

    public Locator resultMessage() {
        return page.locator("#openAccountResult p").first();
    }

    /** Link with the number of the account that was just opened. */
    public Locator newAccountId() {
        return page.locator("#newAccountId");
    }

    /** Waits for the confirmation and returns the new account number. */
    public String newAccountNumber() {
        newAccountId().waitFor();
        return newAccountId().textContent().trim();
    }

    @Step("Open the details of the new account")
    public AccountActivityPage openNewAccountDetails() {
        newAccountId().click();
        return new AccountActivityPage(page);
    }

    private OpenAccountPage submit(AccountType type, String fromAccountId) {
        accountTypeSelect.selectOption(new SelectOption().setLabel(type.name()));
        if (fromAccountId != null) {
            // selectOption waits until the AJAX list contains this account
            fromAccountSelect.selectOption(fromAccountId);
        }
        openAccountButton.click();
        return this;
    }
}
