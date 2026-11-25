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
import parabank.api.ParaBankApiClient;
import parabank.data.AccountType;
import parabank.data.Customer;
import parabank.data.TestUsers;
import parabank.utils.BaseApiTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("Accounts")
@Feature("Accounts API")
@DisplayName("Accounts API")
class AccountsApiTest extends BaseApiTest {

    private static Customer customer;

    private CustomerDto apiCustomer;
    private AccountDto sourceAccount;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @BeforeEach
    void loadCustomer() {
        apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        sourceAccount = api.accountsOf(apiCustomer.id).get(0);
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /createAccount opens an account of the requested type for the customer")
    void createAccountReturnsNewAccount() {
        APIResponse response = api.createAccount(apiCustomer.id, AccountType.SAVINGS, sourceAccount.id);
        assertEquals(200, response.status(), "status code");

        AccountDto created = ParaBankApiClient.parse(response, AccountDto.class);
        assertAll(
                () -> assertTrue(created.id > 0, "account id should be positive"),
                () -> assertNotEquals(sourceAccount.id, created.id, "a new account number is generated"),
                () -> assertEquals(apiCustomer.id, created.customerId, "owner"),
                () -> assertEquals(AccountType.SAVINGS.name(), created.type, "type"));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("A new account is listed among the customer's accounts")
    void newAccountIsListed() {
        AccountDto created = api.openAccount(apiCustomer.id, AccountType.CHECKING, sourceAccount.id);

        assertTrue(api.accountsOf(apiCustomer.id).stream().anyMatch(account -> account.id == created.id),
                "account " + created.id + " should be in the customer's accounts");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /accounts/{id} returns the same id, owner and type as the account creation")
    void getAccountMatchesCreatedAccount() {
        AccountDto created = api.openAccount(apiCustomer.id, AccountType.CHECKING, sourceAccount.id);

        AccountDto fetched = api.account(created.id);

        // The balance is checked separately: the creation response reports it wrong (BUG-002)
        assertAll(
                () -> assertEquals(created.id, fetched.id, "id"),
                () -> assertEquals(created.customerId, fetched.customerId, "owner"),
                () -> assertEquals(created.type, fetched.type, "type"));
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("The initial deposit moves from the source account to the new account: no money is created or lost")
    void initialDepositComesFromSourceAccount() {
        BigDecimal sourceBefore = api.account(sourceAccount.id).balance;

        AccountDto created = api.openAccount(apiCustomer.id, AccountType.SAVINGS, sourceAccount.id);
        // Read the stored balance: the creation response reports 0 (BUG-002)
        BigDecimal deposit = api.account(created.id).balance;
        BigDecimal sourceAfter = api.account(sourceAccount.id).balance;

        assertAll(
                () -> assertTrue(deposit.signum() > 0, "the new account should start with a deposit, got " + deposit),
                () -> assertEquals(0, sourceBefore.subtract(deposit).compareTo(sourceAfter),
                        "source balance: before " + sourceBefore + ", deposit " + deposit + ", after " + sourceAfter));
    }

    /**
     * Known defect BUG-002 (docs/defects.md): the createAccount response reports a balance of 0,
     * although the stored account already holds the initial deposit.
     * Excluded from the default run; run it with: mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @Test
    @Tag("known-defect")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("BUG-002 · The createAccount response shows the real starting balance")
    void createAccountResponseShowsStartingBalance() {
        AccountDto created = api.openAccount(apiCustomer.id, AccountType.CHECKING, sourceAccount.id);

        BigDecimal stored = api.account(created.id).balance;

        assertEquals(0, stored.compareTo(created.balance),
                "balance in the creation response " + created.balance + " vs. stored balance " + stored);
    }
}
