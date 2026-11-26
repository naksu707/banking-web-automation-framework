package parabank.tests.api;

import com.microsoft.playwright.APIResponse;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import parabank.api.BillPayResultDto;
import parabank.api.CustomerDto;
import parabank.api.ParaBankApiClient;
import parabank.data.Customer;
import parabank.data.Payee;
import parabank.data.TestDataFactory;
import parabank.data.TestUsers;
import parabank.utils.BaseApiTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("Payments")
@Feature("Bill pay API")
@DisplayName("Bill pay API")
class BillPayApiTest extends BaseApiTest {

    private static final BigDecimal AMOUNT = new BigDecimal("60.00");

    private static Customer customer;

    private long accountId;
    private Payee payee;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @BeforeEach
    void prepare() {
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        accountId = api.accountsOf(apiCustomer.id).get(0).id;
        payee = TestDataFactory.newPayee();
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("POST /billpay returns 200 with the payee, the amount and the account")
    void billPaySucceeds() {
        APIResponse response = api.billPay(accountId, AMOUNT.toPlainString(), payee);

        assertEquals(200, response.status(), "status code, body: " + response.text());
        BillPayResultDto result = ParaBankApiClient.parse(response, BillPayResultDto.class);
        assertAll(
                () -> assertEquals(payee.name(), result.payeeName, "payee name"),
                () -> assertEquals(0, AMOUNT.compareTo(result.amount), "amount, got " + result.amount),
                () -> assertEquals(accountId, result.accountId, "account id"));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A bill payment takes exactly the amount out of the account")
    void billPayDebitsExactAmount() {
        BigDecimal before = api.balanceOf(accountId);

        api.billPay(accountId, AMOUNT.toPlainString(), payee);

        BigDecimal after = api.balanceOf(accountId);
        assertEquals(0, before.subtract(AMOUNT).compareTo(after), "balance: before " + before + ", after " + after);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Tag("known-defect")
    @DisplayName("BUG-008 · A negative amount is rejected and no money moves")
    void negativeAmountIsRejected() {
        assertPaymentRejected("-60.00");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Tag("known-defect")
    @DisplayName("BUG-009 · An amount greater than the balance is rejected and no money moves")
    void amountAboveBalanceIsRejected() {
        assertPaymentRejected(api.balanceOf(accountId).add(new BigDecimal("1000")).toPlainString());
    }

    /*
     * The two tests above are tagged known-defect: ParaBank accepts these payments (BUG-008 and BUG-009,
     * docs/defects.md). They are excluded from the default run; run them with:
     * mvn test -Dgroups=known-defect -DexcludedGroups=
     */

    /** The API answers with a client error, and the balance stays the same. */
    private void assertPaymentRejected(String amount) {
        BigDecimal before = api.balanceOf(accountId);

        APIResponse response = api.billPay(accountId, amount, payee);

        BigDecimal after = api.balanceOf(accountId);
        assertAll(
                () -> assertEquals(400, response.status(), "status code, body: " + response.text()),
                () -> assertEquals(0, before.compareTo(after), "balance: before " + before + ", after " + after));
    }
}
