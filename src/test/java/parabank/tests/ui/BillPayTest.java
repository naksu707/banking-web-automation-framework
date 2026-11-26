package parabank.tests.ui;

import com.microsoft.playwright.APIRequestContext;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import parabank.api.CustomerDto;
import parabank.api.ParaBankApiClient;
import parabank.data.Customer;
import parabank.data.Payee;
import parabank.data.TestDataFactory;
import parabank.data.TestUsers;
import parabank.pages.BillPayPage;
import parabank.pages.HomePage;
import parabank.utils.BaseTest;

import java.math.BigDecimal;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("Payments")
@Feature("Bill pay")
@DisplayName("Bill pay")
class BillPayTest extends BaseTest {

    /** Every validation message that an empty form must show. */
    private static final List<String> REQUIRED_FIELD_MESSAGES = List.of(
            "name", "address", "city", "state", "zipCode", "phoneNumber",
            "account-empty", "verifyAccount-empty", "amount-empty");

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A bill payment shows the confirmation with the payee, the amount and the account")
    void billPaymentShowsConfirmation(APIRequestContext request) {
        String accountId = firstAccountId(request);
        Payee payee = TestDataFactory.newPayee();

        BillPayPage billPay = loginAndGoToBillPay().pay(payee, "45", accountId);

        assertThat(billPay.resultTitle()).hasText("Bill Payment Complete");
        assertThat(billPay.payeeNameResult()).hasText(payee.name());
        assertThat(billPay.amountResult()).hasText("$45.00");
        assertThat(billPay.fromAccountResult()).hasText(accountId);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("An empty form shows a required-field message for every field")
    void emptyFormShowsRequiredMessages() {
        BillPayPage billPay = loginAndGoToBillPay().send();

        for (String key : REQUIRED_FIELD_MESSAGES) {
            assertThat(billPay.validationMessage(key)).isVisible();
        }
        assertThat(billPay.resultTitle()).isHidden();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Different account numbers are rejected and no money leaves the account")
    void accountMismatchIsRejected(APIRequestContext request) {
        Payee payee = TestDataFactory.newPayee();
        assertPaymentRejected(request, payee, payee.accountNumber() + "1", "45", "verifyAccount-mismatch");
    }

    @ParameterizedTest(name = "Amount \"{0}\" shows \"{1}\" and no money leaves the account")
    @CsvSource({"'', amount-empty", "abc, amount-invalid"})
    @Severity(SeverityLevel.CRITICAL)
    void invalidAmountIsRejected(String amount, String message, APIRequestContext request) {
        Payee payee = TestDataFactory.newPayee();
        assertPaymentRejected(request, payee, payee.accountNumber(), amount, message);
    }

    /**
     * Known defects BUG-008 (negative amount) and BUG-010 (zero amount), see docs/defects.md:
     * the page confirms these payments instead of rejecting them.
     * Excluded from the default run; run it with: mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @ParameterizedTest(name = "BUG-008/010 · Amount \"{0}\" is rejected and no money leaves the account")
    @ValueSource(strings = {"-20", "0"})
    @Tag("known-defect")
    @Severity(SeverityLevel.CRITICAL)
    void negativeOrZeroAmountIsRejected(String amount, APIRequestContext request) {
        Payee payee = TestDataFactory.newPayee();
        assertPaymentRejected(request, payee, payee.accountNumber(), amount, null);
    }

    /**
     * Sends the form and checks that the payment was not made: the expected validation message is visible
     * (when one is given), there is no confirmation, and the balance stays the same (checked through the API).
     */
    private void assertPaymentRejected(APIRequestContext request, Payee payee, String verifyAccount,
                                       String amount, String expectedMessage) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        String accountId = firstAccountId(request);
        BigDecimal before = api.balanceOf(Long.parseLong(accountId));

        BillPayPage billPay = loginAndGoToBillPay()
                .fill(payee, verifyAccount, amount, accountId)
                .send();
        billPay.anyResponse().waitFor();

        if (expectedMessage != null) {
            assertThat(billPay.validationMessage(expectedMessage)).isVisible();
        }
        assertThat(billPay.resultTitle()).isHidden();
        BigDecimal after = api.balanceOf(Long.parseLong(accountId));
        assertEquals(0, before.compareTo(after), "balance: before " + before + ", after " + after);
    }

    private BillPayPage loginAndGoToBillPay() {
        return new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToBillPay();
    }

    private String firstAccountId(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        return String.valueOf(api.accountsOf(apiCustomer.id).get(0).id);
    }
}
