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
import parabank.data.TestUsers;
import parabank.pages.AccountsOverviewPage;
import parabank.pages.HomePage;
import parabank.utils.BaseTest;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Validates that the UI and the REST API show the same data for the same customer. */
@Tag("integration")
@Epic("Accounts")
@Feature("UI and API consistency")
@DisplayName("Accounts: UI vs. API")
class AccountsAcrossLayersTest extends BaseTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("The accounts overview lists the same accounts returned by the API")
    void overviewMatchesApiAccounts(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        List<String> apiAccountIds = api.accountsOf(apiCustomer.id).stream()
                .map(account -> String.valueOf(account.id))
                .sorted()
                .toList();

        AccountsOverviewPage overview = new HomePage(page).open()
                .loginAs(customer.username(), customer.password());
        assertThat(overview.accountLinks().first()).isVisible();

        List<String> uiAccountIds = overview.accountLinks().allTextContents().stream()
                .map(String::trim)
                .sorted()
                .toList();

        assertEquals(apiAccountIds, uiAccountIds, "account numbers shown in the UI vs. returned by the API");
    }
}
