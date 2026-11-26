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
import org.junit.jupiter.params.provider.CsvFileSource;
import parabank.api.CustomerDto;
import parabank.api.ParaBankApiClient;
import parabank.data.Customer;
import parabank.data.TestUsers;
import parabank.pages.HomePage;
import parabank.pages.RequestLoanPage;
import parabank.utils.BaseTest;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ParaBank approves a loan when the down payment is not greater than the customer's available funds
 * and the available funds are at least 20% of the loan amount. A new customer has $515.50 in one account.
 */
@Epic("Loans")
@Feature("Request loan")
@DisplayName("Request loan")
class RequestLoanTest extends BaseTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A loan within the customer's funds is approved and a new account is shown")
    void loanWithinFundsIsApproved(APIRequestContext request) {
        RequestLoanPage loan = loginAndGoToRequestLoan()
                .requestLoan("1000", "50", firstAccountId(request));

        assertThat(loan.resultTitle()).hasText("Loan Request Processed");
        assertThat(loan.status()).hasText("Approved");
        assertThat(loan.approvedMessage()).hasText("Congratulations, your loan has been approved.");
        assertThat(loan.newAccountId()).hasText(Pattern.compile("\\d+"));
    }

    @ParameterizedTest(name = "Loan {0} with down payment {1} is denied: \"{2}\"")
    @CsvFileSource(resources = "/data/loan-denials.csv", numLinesToSkip = 1, delimiter = '|')
    @Severity(SeverityLevel.CRITICAL)
    void loanOutsideTheRulesIsDenied(String amount, String downPayment, String message, APIRequestContext request) {
        int accountsBefore = accountCount(request);

        RequestLoanPage loan = loginAndGoToRequestLoan()
                .requestLoan(amount, downPayment, firstAccountId(request));

        assertThat(loan.status()).hasText("Denied");
        assertThat(loan.deniedMessage()).hasText(message);
        assertThat(loan.approvedMessage()).isHidden();
        assertEquals(accountsBefore, accountCount(request), "a new account was created for a denied loan");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Empty fields are rejected and no account is created")
    void emptyFieldsAreRejected(APIRequestContext request) {
        int accountsBefore = accountCount(request);

        RequestLoanPage loan = loginAndGoToRequestLoan().requestLoan("", "", firstAccountId(request));
        loan.anyResponse().waitFor();

        assertThat(loan.approvedMessage()).isHidden();
        assertEquals(accountsBefore, accountCount(request), "a new account was created");
    }

    private RequestLoanPage loginAndGoToRequestLoan() {
        return new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToRequestLoan();
    }

    private String firstAccountId(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        return String.valueOf(api.accountsOf(apiCustomer.id).get(0).id);
    }

    private int accountCount(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        return api.accountsOf(apiCustomer.id).size();
    }
}
