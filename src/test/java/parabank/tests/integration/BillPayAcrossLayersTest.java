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

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pays a bill in the UI and checks the balance in the back end through the API. */
@Tag("integration")
@Epic("Payments")
@Feature("UI and API consistency")
@DisplayName("Bill pay: UI vs. API")
class BillPayAcrossLayersTest extends BaseTest {

    private static final BigDecimal AMOUNT = new BigDecimal("27.75");

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    /**
     * Known defect BUG-011, see docs/defects.md: intermittently, the payment is confirmed
     * but the balance does not change, neither in the API nor in the UI.
     * Excluded from the default run; run it with: mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @Test
    @Tag("known-defect")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("BUG-011 · A bill paid in the UI takes exactly the amount out of the account")
    void uiBillPaymentDebitsAccount(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        long accountId = api.accountsOf(apiCustomer.id).get(0).id;
        BigDecimal before = api.balanceOf(accountId);
        Payee payee = TestDataFactory.newPayee();

        BillPayPage billPay = new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToBillPay()
                .pay(payee, AMOUNT.toPlainString(), String.valueOf(accountId));
        assertThat(billPay.resultTitle()).hasText("Bill Payment Complete");
        // The confirmation must name the account that was chosen
        assertThat(billPay.fromAccountResult()).hasText(String.valueOf(accountId));

        BigDecimal expected = before.subtract(AMOUNT);
        BigDecimal after = api.awaitBalance(accountId, expected);
        if (expected.compareTo(after) != 0) {
            // Diagnosis: does the UI show the debit even though the API does not?
            String uiBalance = billPay.goToAccountsOverview().balanceText(String.valueOf(accountId));
            assertEquals(0, expected.compareTo(after), "balance in the API: before " + before + ", after " + after
                    + " (expected " + expected + "); balance in the UI Accounts Overview: " + uiBalance);
        }
    }
}
