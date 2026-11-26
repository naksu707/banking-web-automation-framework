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
import parabank.api.LoanResponseDto;
import parabank.api.ParaBankApiClient;
import parabank.data.AccountType;
import parabank.data.Customer;
import parabank.data.TestUsers;
import parabank.utils.BaseApiTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loan rules in ParaBank: the down payment cannot be greater than the available funds
 * (sum of the balances of all non-loan accounts), and the available funds must cover a minimum share of the loan
 * (an admin setting, 20% by default). The approved loans here stay well inside those limits.
 * All tests share one customer: approved loans lower the available funds a little, but every test reads
 * the balances before it starts and the denied cases stay far outside the limits.
 */
@Epic("Loans")
@Feature("Loans API")
@DisplayName("Loans API")
class LoanApiTest extends BaseApiTest {

    private static final BigDecimal LOAN = new BigDecimal("500.00");
    private static final BigDecimal DOWN_PAYMENT = new BigDecimal("50.00");

    private static Customer customer;

    private long customerId;
    private long accountId;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @BeforeEach
    void firstAccount() {
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        customerId = apiCustomer.id;
        accountId = api.accountsOf(customerId).get(0).id;
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A loan within the customer's funds is approved and creates a LOAN account with the loan amount")
    void approvedLoanCreatesLoanAccount() {
        LoanResponseDto loan = request(LOAN, DOWN_PAYMENT, accountId);

        assertTrue(loan.approved, "loan should be approved, message: " + loan.message);
        assertNotNull(loan.accountId, "new account id");
        AccountDto loanAccount = api.account(loan.accountId);
        assertAll(
                () -> assertEquals("LOAN", loanAccount.type, "type"),
                () -> assertEquals(customerId, loanAccount.customerId, "owner"),
                () -> assertEquals(0, LOAN.compareTo(loanAccount.balance), "balance, got " + loanAccount.balance));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("An approved loan takes the down payment out of the source account")
    void approvedLoanTakesDownPayment() {
        BigDecimal before = api.balanceOf(accountId);

        LoanResponseDto loan = request(LOAN, DOWN_PAYMENT, accountId);

        assertTrue(loan.approved, "loan should be approved, message: " + loan.message);
        BigDecimal after = api.balanceOf(accountId);
        assertEquals(0, before.subtract(DOWN_PAYMENT).compareTo(after), "balance: before " + before + ", after " + after);
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("A loan more than 5 times the available funds is denied and nothing changes")
    void loanAboveFundsIsDenied() {
        assertDenied(new BigDecimal("10000.00"), DOWN_PAYMENT, "error.insufficient.funds");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("A down payment greater than the available funds is denied and nothing changes")
    void downPaymentAboveFundsIsDenied() {
        assertDenied(new BigDecimal("2000.00"), new BigDecimal("1000.00"), "error.insufficient.funds.for.down.payment");
    }

    /**
     * Known defect BUG-013, see docs/defects.md: the loan is approved and the negative down payment
     * is added to the source account. Excluded from the default run; run it with:
     * mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @Test
    @Tag("known-defect")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BUG-013 · A negative down payment is rejected and nothing changes")
    void negativeDownPaymentIsRejected() {
        assertRejected(LOAN, new BigDecimal("-200.00"), accountId);
    }

    /**
     * Known defect BUG-012, see docs/defects.md: the down payment is only checked against the customer's total funds,
     * so it can overdraw the source account. Uses its own customer, because the defect leaves an account overdrawn.
     * Excluded from the default run; run it with: mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @Test
    @Tag("known-defect")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BUG-012 · A down payment greater than the source account balance is rejected")
    void downPaymentAboveSourceBalanceIsRejected() {
        Customer own = TestUsers.registerNewCustomer();
        customerId = api.loginAndGetCustomer(own.username(), own.password()).id;
        accountId = api.accountsOf(customerId).get(0).id;
        // A second account with $100: the customer's total funds still cover the down payment, this account does not
        long smallAccount = api.openAccount(customerId, AccountType.CHECKING, accountId).id;

        assertRejected(new BigDecimal("1000.00"), new BigDecimal("300.00"), smallAccount);
    }

    private LoanResponseDto request(BigDecimal amount, BigDecimal downPayment, long fromAccountId) {
        APIResponse response = api.requestLoan(customerId, amount.toPlainString(), downPayment.toPlainString(), fromAccountId);
        assertEquals(200, response.status(), "status code, body: " + response.text());
        return ParaBankApiClient.parse(response, LoanResponseDto.class);
    }

    /** The loan is denied with the given message, no account is created and the balance stays the same. */
    private void assertDenied(BigDecimal amount, BigDecimal downPayment, String message) {
        int accountsBefore = api.accountsOf(customerId).size();
        BigDecimal before = api.balanceOf(accountId);

        LoanResponseDto loan = request(amount, downPayment, accountId);

        assertAll(
                () -> assertFalse(loan.approved, "loan should be denied"),
                () -> assertEquals(message, loan.message, "message"),
                () -> assertEquals(accountsBefore, api.accountsOf(customerId).size(), "a new account was created"),
                () -> assertEquals(0, before.compareTo(api.balanceOf(accountId)), "source balance changed"));
    }

    /** The loan is not granted (client error or denied), no account is created and the source balance stays the same. */
    private void assertRejected(BigDecimal amount, BigDecimal downPayment, long fromAccountId) {
        int accountsBefore = api.accountsOf(customerId).size();
        BigDecimal before = api.balanceOf(fromAccountId);

        APIResponse response = api.requestLoan(customerId, amount.toPlainString(), downPayment.toPlainString(), fromAccountId);
        boolean approved = response.status() == 200
                && ParaBankApiClient.parse(response, LoanResponseDto.class).approved;

        BigDecimal after = api.balanceOf(fromAccountId);
        assertAll(
                () -> assertFalse(approved, "loan should not be approved, response: " + response.status() + " " + response.text()),
                () -> assertEquals(accountsBefore, api.accountsOf(customerId).size(), "a new account was created"),
                () -> assertEquals(0, before.compareTo(after), "source balance: before " + before + ", after " + after));
    }
}
