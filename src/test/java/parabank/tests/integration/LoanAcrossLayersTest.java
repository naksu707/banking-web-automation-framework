package parabank.tests.integration;

import com.microsoft.playwright.APIRequestContext;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import parabank.api.AccountDto;
import parabank.api.CustomerDto;
import parabank.api.ParaBankApiClient;
import parabank.data.Customer;
import parabank.data.TestUsers;
import parabank.pages.HomePage;
import parabank.pages.RequestLoanPage;
import parabank.utils.BaseTest;

import java.math.BigDecimal;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Requests a loan in the UI and checks the new account and the down payment in the back end through the API. */
@Tag("integration")
@Epic("Loans")
@Feature("UI and API consistency")
@DisplayName("Request loan: UI vs. API")
class LoanAcrossLayersTest extends BaseTest {

    private static final BigDecimal LOAN = new BigDecimal("1200.00");
    private static final BigDecimal DOWN_PAYMENT = new BigDecimal("80.00");

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A loan approved in the UI exists in the back end and the down payment was taken")
    void uiLoanMatchesBackEnd(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        long fromId = api.accountsOf(apiCustomer.id).get(0).id;
        BigDecimal before = api.balanceOf(fromId);

        RequestLoanPage loan = new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToRequestLoan()
                .requestLoan(LOAN.toPlainString(), DOWN_PAYMENT.toPlainString(), String.valueOf(fromId));
        assertThat(loan.status()).hasText("Approved");
        long loanId = Long.parseLong(loan.newAccountId().innerText().trim());

        AccountDto loanAccount = api.account(loanId);
        BigDecimal expected = before.subtract(DOWN_PAYMENT);
        BigDecimal after = api.awaitBalance(fromId, expected);
        assertAll(
                () -> assertEquals("LOAN", loanAccount.type, "type"),
                () -> assertEquals(apiCustomer.id, loanAccount.customerId, "owner"),
                () -> assertEquals(0, LOAN.compareTo(loanAccount.balance), "loan balance, got " + loanAccount.balance),
                () -> assertEquals(0, expected.compareTo(after), "source: before " + before + ", after " + after));
    }
}
