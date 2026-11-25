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
import parabank.api.AccountDto;
import parabank.api.CustomerDto;
import parabank.data.AccountType;
import parabank.data.Customer;
import parabank.data.TestUsers;
import parabank.utils.BaseApiTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("Transfers")
@Feature("Transfer API")
@DisplayName("Transfer API")
class TransferApiTest extends BaseApiTest {

    private static final BigDecimal AMOUNT = new BigDecimal("40.00");

    private static Customer customer;

    private long fromId;
    private long toId;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    /** Every test gets the customer's first account and a brand-new second account. */
    @BeforeEach
    void prepareAccounts() {
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        AccountDto first = api.accountsOf(apiCustomer.id).get(0);
        fromId = first.id;
        toId = api.openAccount(apiCustomer.id, AccountType.CHECKING, first.id).id;
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("POST /transfer between own accounts returns 200 and a confirmation")
    void transferSucceeds() {
        APIResponse response = api.transfer(fromId, toId, AMOUNT.toPlainString());

        assertEquals(200, response.status(), "status code, body: " + response.text());
        assertTrue(response.text().contains("Successfully transferred"), "confirmation, got: " + response.text());
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A transfer moves exactly the amount: source − amount and destination + amount")
    void transferMovesExactAmount() {
        BigDecimal fromBefore = api.balanceOf(fromId);
        BigDecimal toBefore = api.balanceOf(toId);

        api.transfer(fromId, toId, AMOUNT.toPlainString());

        BigDecimal fromAfter = api.balanceOf(fromId);
        BigDecimal toAfter = api.balanceOf(toId);
        assertAll(
                () -> assertEquals(0, fromBefore.subtract(AMOUNT).compareTo(fromAfter),
                        "source: before " + fromBefore + ", after " + fromAfter),
                () -> assertEquals(0, toBefore.add(AMOUNT).compareTo(toAfter),
                        "destination: before " + toBefore + ", after " + toAfter));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Tag("known-defect")
    @DisplayName("BUG-004 · A negative amount is rejected and no money moves")
    void negativeAmountIsRejected() {
        assertTransferRejected(fromId, toId, "-40.00");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Tag("known-defect")
    @DisplayName("BUG-006 · A zero amount is rejected and no money moves")
    void zeroAmountIsRejected() {
        assertTransferRejected(fromId, toId, "0");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @Tag("known-defect")
    @DisplayName("BUG-005 · An amount greater than the source balance is rejected and no money moves")
    void amountAboveBalanceIsRejected() {
        BigDecimal tooMuch = api.balanceOf(fromId).add(new BigDecimal("1000.00"));

        assertTransferRejected(fromId, toId, tooMuch.toPlainString());
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @Tag("known-defect")
    @DisplayName("BUG-007 · A transfer to the same account is rejected")
    void sameAccountTransferIsRejected() {
        assertTransferRejected(fromId, fromId, AMOUNT.toPlainString());
    }

    /*
     * The four tests above are tagged known-defect: ParaBank accepts these transfers (BUG-004 to BUG-007,
     * docs/defects.md). They are excluded from the default run; run them with:
     * mvn test -Dgroups=known-defect -DexcludedGroups=
     */

    /** The API answers with a client error, and both balances stay the same. */
    private void assertTransferRejected(long from, long to, String amount) {
        BigDecimal fromBefore = api.balanceOf(from);
        BigDecimal toBefore = api.balanceOf(to);

        APIResponse response = api.transfer(from, to, amount);

        BigDecimal fromAfter = api.balanceOf(from);
        BigDecimal toAfter = api.balanceOf(to);
        assertAll(
                () -> assertEquals(400, response.status(), "status code, body: " + response.text()),
                () -> assertEquals(0, fromBefore.compareTo(fromAfter),
                        "source balance changed: before " + fromBefore + ", after " + fromAfter),
                () -> assertEquals(0, toBefore.compareTo(toAfter),
                        "destination balance changed: before " + toBefore + ", after " + toAfter));
    }
}
