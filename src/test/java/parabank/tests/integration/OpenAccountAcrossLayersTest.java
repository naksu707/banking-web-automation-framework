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
import parabank.data.AccountType;
import parabank.data.Customer;
import parabank.data.TestUsers;
import parabank.pages.HomePage;
import parabank.pages.OpenAccountPage;
import parabank.utils.BaseTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opens an account in the UI and checks the result in the back end through the API. */
@Tag("integration")
@Epic("Accounts")
@Feature("UI and API consistency")
@DisplayName("Open account: UI vs. API")
class OpenAccountAcrossLayersTest extends BaseTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("An account opened in the UI exists in the API with the right owner, type and funding")
    void accountOpenedInUiIsStoredCorrectly(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        AccountDto source = api.accountsOf(apiCustomer.id).get(0);
        BigDecimal sourceBefore = source.balance;

        OpenAccountPage openAccount = new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToOpenNewAccount()
                .openAccount(AccountType.CHECKING, String.valueOf(source.id));
        long newAccountId = Long.parseLong(openAccount.newAccountNumber());

        AccountDto created = api.account(newAccountId);
        BigDecimal sourceAfter = api.account(source.id).balance;

        assertAll(
                () -> assertEquals(apiCustomer.id, created.customerId, "owner"),
                () -> assertEquals(AccountType.CHECKING.name(), created.type, "type"),
                () -> assertTrue(created.balance.signum() > 0, "initial deposit, got " + created.balance),
                () -> assertEquals(0, sourceBefore.subtract(created.balance).compareTo(sourceAfter),
                        "source balance: before " + sourceBefore + ", deposit " + created.balance
                                + ", after " + sourceAfter));
    }
}
