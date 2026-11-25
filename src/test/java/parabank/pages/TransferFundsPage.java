package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/**
 * "Transfer Funds" page.
 * The account lists are loaded with AJAX and the result is shown on the same page, without navigation.
 */
public class TransferFundsPage extends BasePage {

    private final Locator amountInput;
    private final Locator fromAccountSelect;
    private final Locator toAccountSelect;
    private final Locator transferButton;

    public TransferFundsPage(Page page) {
        super(page);
        this.amountInput = page.locator("#amount");
        this.fromAccountSelect = page.locator("#fromAccountId");
        this.toAccountSelect = page.locator("#toAccountId");
        this.transferButton = page.locator("input[value='Transfer']");
    }

    @Step("Open the \"Transfer Funds\" page")
    public TransferFundsPage open() {
        page.navigate("transfer.htm");
        return this;
    }

    @Step("Transfer \"{amount}\" from account {fromAccountId} to account {toAccountId}")
    public TransferFundsPage transfer(String amount, String fromAccountId, String toAccountId) {
        // selectOption waits until the AJAX lists contain both accounts
        fromAccountSelect.selectOption(fromAccountId);
        toAccountSelect.selectOption(toAccountId);
        amountInput.fill(amount);
        transferButton.click();
        return this;
    }

    public Locator resultTitle() {
        return page.locator("#showResult h1.title");
    }

    public Locator amountResult() {
        return page.locator("#amountResult");
    }

    public Locator fromAccountResult() {
        return page.locator("#fromAccountIdResult");
    }

    public Locator toAccountResult() {
        return page.locator("#toAccountIdResult");
    }

    /**
     * Whatever the page shows after submitting: the confirmation, the error panel or a field validation message.
     * Waiting for it makes sure the page has answered before checking what it answered.
     */
    public Locator anyResponse() {
        return page.locator("#showResult:visible, #showError:visible, #rightPanel .error:visible").first();
    }
}
