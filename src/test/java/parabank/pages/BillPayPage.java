package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import parabank.data.Payee;

/**
 * "Bill Pay" page.
 * The form is validated in the browser; when it is valid, the payment is sent with AJAX
 * and the result is shown on the same page, without navigation.
 */
public class BillPayPage extends BasePage {

    public BillPayPage(Page page) {
        super(page);
    }

    @Step("Pay \"{amount}\" to {payee.name} from account {fromAccountId}")
    public BillPayPage pay(Payee payee, String amount, String fromAccountId) {
        return fill(payee, payee.accountNumber(), amount, fromAccountId).send();
    }

    /** Fills the whole form; "verifyAccount" is separate so tests can make it differ from the payee account. */
    @Step("Fill the bill payment form")
    public BillPayPage fill(Payee payee, String verifyAccount, String amount, String fromAccountId) {
        field("payee.name").fill(payee.name());
        field("payee.address.street").fill(payee.street());
        field("payee.address.city").fill(payee.city());
        field("payee.address.state").fill(payee.state());
        field("payee.address.zipCode").fill(payee.zipCode());
        // The phone input gets a random id and class on every load, so it is located by name
        field("payee.phoneNumber").fill(payee.phoneNumber());
        field("payee.accountNumber").fill(payee.accountNumber());
        field("verifyAccount").fill(verifyAccount);
        field("amount").fill(amount);
        page.locator("select[name='fromAccountId']").selectOption(fromAccountId);
        return this;
    }

    @Step("Click \"Send Payment\"")
    public BillPayPage send() {
        page.locator("input[value='Send Payment']").click();
        return this;
    }

    public Locator resultTitle() {
        return page.locator("#billpayResult h1.title");
    }

    public Locator payeeNameResult() {
        return page.locator("#payeeName");
    }

    public Locator amountResult() {
        return page.locator("#billpayResult #amount");
    }

    public Locator fromAccountResult() {
        return page.locator("#billpayResult #fromAccountId");
    }

    /**
     * Validation message next to a field, by the suffix of its id:
     * name, address, city, state, zipCode, phoneNumber, account-empty, account-invalid,
     * verifyAccount-empty, verifyAccount-invalid, verifyAccount-mismatch, amount-empty, amount-invalid.
     */
    public Locator validationMessage(String key) {
        return page.locator("#validationModel-" + key);
    }

    /** Whatever the page shows after "Send Payment": the confirmation, the error panel or a validation message. */
    public Locator anyResponse() {
        return page.locator("#billpayResult:visible, #billpayError:visible, [id^='validationModel']:visible").first();
    }

    private Locator field(String name) {
        return page.locator("[name='" + name + "']");
    }
}
