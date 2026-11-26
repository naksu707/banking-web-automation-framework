package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

/**
 * "Request Loan" page.
 * The request is sent with AJAX and the result is shown on the same page, without navigation.
 * The form has no validation in the browser.
 */
public class RequestLoanPage extends BasePage {

    public RequestLoanPage(Page page) {
        super(page);
    }

    @Step("Request a loan of \"{amount}\" with a down payment of \"{downPayment}\" from account {fromAccountId}")
    public RequestLoanPage requestLoan(String amount, String downPayment, String fromAccountId) {
        page.locator("#amount").fill(amount);
        page.locator("#downPayment").fill(downPayment);
        page.locator("#fromAccountId").selectOption(fromAccountId);
        page.locator("input[value='Apply Now']").click();
        return this;
    }

    public Locator resultTitle() {
        return page.locator("#requestLoanResult h1.title");
    }

    /** "Approved" or "Denied". */
    public Locator status() {
        return page.locator("#loanStatus");
    }

    public Locator approvedMessage() {
        return page.locator("#loanRequestApproved p").first();
    }

    public Locator deniedMessage() {
        return page.locator("#loanRequestDenied p.error");
    }

    public Locator newAccountId() {
        return page.locator("#newAccountId");
    }

    public Locator errorPanel() {
        return page.locator("#requestLoanError");
    }

    /** Whatever the page shows after "Apply Now": the result or the error panel. */
    public Locator anyResponse() {
        return page.locator("#requestLoanResult:visible, #requestLoanError:visible").first();
    }
}
