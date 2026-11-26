package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/**
 * "Find Transactions" page. Each search is validated in the browser, sent with AJAX,
 * and the results replace the form on the same page.
 */
public class FindTransactionsPage extends BasePage {

    public FindTransactionsPage(Page page) {
        super(page);
    }

    @Step("Select account {accountId}")
    public FindTransactionsPage selectAccount(String accountId) {
        page.locator("#accountId").selectOption(accountId);
        return this;
    }

    @Step("Find by transaction id \"{transactionId}\"")
    public FindTransactionsPage findById(String transactionId) {
        page.locator("#transactionId").fill(transactionId);
        page.locator("#findById").click();
        return this;
    }

    @Step("Find by amount \"{amount}\"")
    public FindTransactionsPage findByAmount(String amount) {
        page.locator("#amount").fill(amount);
        page.locator("#findByAmount").click();
        return this;
    }

    @Step("Find by date \"{date}\"")
    public FindTransactionsPage findByDate(String date) {
        page.locator("#transactionDate").fill(date);
        page.locator("#findByDate").click();
        return this;
    }

    @Step("Find from \"{fromDate}\" to \"{toDate}\"")
    public FindTransactionsPage findByDateRange(String fromDate, String toDate) {
        page.locator("#fromDate").fill(fromDate);
        page.locator("#toDate").fill(toDate);
        page.locator("#findByDateRange").click();
        return this;
    }

    public Locator resultTitle() {
        return page.locator("#resultContainer h1.title");
    }

    /** Rows of the results table. */
    public Locator rows() {
        return page.locator("#transactionBody tr");
    }

    /** Result row whose description is exactly the given text. */
    public Locator row(String description) {
        return page.locator("#transactionBody tr:has(a:text-is('" + description + "'))");
    }

    /** Debit cell (third column) of a row. */
    public Locator debit(Locator row) {
        return row.locator("td").nth(2);
    }

    /** Credit cell (fourth column) of a row. */
    public Locator credit(Locator row) {
        return row.locator("td").nth(3);
    }

    /** Validation message next to a search: transactionIdError, transactionDateError, dateRangeError or amountError. */
    public Locator validationMessage(String id) {
        return page.locator("#" + id);
    }

    public Locator errorPanel() {
        return page.locator("#errorContainer");
    }
}
