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
import org.junit.jupiter.params.provider.ValueSource;
import parabank.api.AccountDto;
import parabank.api.CustomerDto;
import parabank.api.ParaBankApiClient;
import parabank.data.AccountType;
import parabank.data.Customer;
import parabank.data.TestUsers;
import parabank.pages.HomePage;
import parabank.pages.TransferFundsPage;
import parabank.utils.BaseTest;

import java.math.BigDecimal;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Epic("Transfers")
@Feature("Transfer funds")
@DisplayName("Transfer funds")
class TransferFundsTest extends BaseTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A transfer between own accounts shows the confirmation with the amount and both accounts")
    void transferShowsConfirmation(APIRequestContext request) {
        Accounts accounts = twoAccounts(request);

        TransferFundsPage transfer = loginAndGoToTransfer()
                .transfer("25", accounts.from(), accounts.to());

        assertThat(transfer.resultTitle()).hasText("Transfer Complete!");
        assertThat(transfer.amountResult()).hasText("$25.00");
        assertThat(transfer.fromAccountResult()).hasText(accounts.from());
        assertThat(transfer.toAccountResult()).hasText(accounts.to());
    }

    @ParameterizedTest(name = "Amount \"{0}\" is rejected and no money moves")
    @ValueSource(strings = {"", "abc"})
    @Severity(SeverityLevel.CRITICAL)
    void invalidAmountIsRejected(String amount, APIRequestContext request) {
        assertTransferRejected(amount, request);
    }

    /**
     * Known defects BUG-004 (negative amount) and BUG-006 (zero amount), see docs/defects.md:
     * the page confirms these transfers instead of rejecting them.
     * Excluded from the default run; run it with: mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @ParameterizedTest(name = "BUG-004/006 · Amount \"{0}\" is rejected and no money moves")
    @ValueSource(strings = {"-50", "0"})
    @Tag("known-defect")
    @Severity(SeverityLevel.CRITICAL)
    void negativeOrZeroAmountIsRejected(String amount, APIRequestContext request) {
        assertTransferRejected(amount, request);
    }

    /** The page answers without a confirmation, and both balances stay the same (checked through the API). */
    private void assertTransferRejected(String amount, APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        Accounts accounts = twoAccounts(request);
        BigDecimal fromBefore = api.balanceOf(accounts.fromId());
        BigDecimal toBefore = api.balanceOf(accounts.toId());

        TransferFundsPage transfer = loginAndGoToTransfer()
                .transfer(amount, accounts.from(), accounts.to());
        transfer.anyResponse().waitFor();

        assertThat(transfer.resultTitle()).isHidden();
        assertAll(
                () -> assertEquals(0, fromBefore.compareTo(api.balanceOf(accounts.fromId())), "source balance changed"),
                () -> assertEquals(0, toBefore.compareTo(api.balanceOf(accounts.toId())), "destination balance changed"));
    }

    private TransferFundsPage loginAndGoToTransfer() {
        return new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToTransferFunds();
    }

    /** The customer's first account plus a new one opened through the API, so every test has two fresh accounts. */
    private Accounts twoAccounts(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        AccountDto first = api.accountsOf(apiCustomer.id).get(0);
        AccountDto second = api.openAccount(apiCustomer.id, AccountType.CHECKING, first.id);
        return new Accounts(first.id, second.id);
    }

    private record Accounts(long fromId, long toId) {
        String from() {
            return String.valueOf(fromId);
        }

        String to() {
            return String.valueOf(toId);
        }
    }
}
