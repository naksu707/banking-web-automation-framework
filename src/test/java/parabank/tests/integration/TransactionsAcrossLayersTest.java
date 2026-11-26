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
import parabank.api.TransactionDto;
import parabank.data.AccountType;
import parabank.data.Customer;
import parabank.data.Payee;
import parabank.data.TestDataFactory;
import parabank.data.TestUsers;
import parabank.pages.FindTransactionsPage;
import parabank.pages.HomePage;
import parabank.utils.BaseTest;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Compares the transaction history shown in the UI with the one returned by the API. */
@Tag("integration")
@Epic("Transactions")
@Feature("UI and API consistency")
@DisplayName("Transactions: UI vs. API")
class TransactionsAcrossLayersTest extends BaseTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("The UI shows the same transactions as the API for an account")
    void uiHistoryMatchesApi(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        long fromId = api.accountsOf(apiCustomer.id).get(0).id;
        long toId = api.openAccount(apiCustomer.id, AccountType.CHECKING, fromId).id;
        api.transfer(fromId, toId, TestDataFactory.uniqueAmount());
        Payee payee = TestDataFactory.newPayee();
        api.billPay(fromId, TestDataFactory.uniqueAmount(), payee);
        List<TransactionDto> history = api.transactionsOf(fromId);

        // A date range that covers any date, so the search returns the whole history
        FindTransactionsPage find = new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToFindTransactions()
                .selectAccount(String.valueOf(fromId))
                .findByDateRange("01-01-2000", "12-31-2099");

        assertThat(find.resultTitle()).hasText("Transaction Results");
        assertThat(find.rows()).hasCount(history.size());
        for (TransactionDto transaction : history) {
            String value = String.format(java.util.Locale.US, "$%.2f", transaction.amount);
            assertThat(find.row(transaction.description)
                    .filter(new com.microsoft.playwright.Locator.FilterOptions().setHasText(value)))
                    .hasCount(1);
        }
    }
}
