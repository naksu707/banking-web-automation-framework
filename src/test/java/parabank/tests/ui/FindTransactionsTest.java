package parabank.tests.ui;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Locator;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import parabank.api.CustomerDto;
import parabank.api.ParaBankApiClient;
import parabank.api.TransactionDto;
import parabank.data.AccountType;
import parabank.data.Customer;
import parabank.data.TestDataFactory;
import parabank.data.TestUsers;
import parabank.pages.FindTransactionsPage;
import parabank.pages.HomePage;
import parabank.utils.BaseTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Each test creates its own transfer through the API, with a unique amount, and looks for it in the UI. */
@Epic("Transactions")
@Feature("Find transactions")
@DisplayName("Find transactions")
class FindTransactionsTest extends BaseTest {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Searching by amount shows the transfer as a debit")
    void findByAmountShowsTransfer(APIRequestContext request) {
        Transfer transfer = transfer(request, customer);

        FindTransactionsPage find = loginAndGoToFindTransactions(customer)
                .selectAccount(String.valueOf(transfer.fromId()))
                .findByAmount(transfer.amount());

        assertThat(find.resultTitle()).hasText("Transaction Results");
        Locator row = find.row("Funds Transfer Sent");
        assertThat(row).hasCount(1);
        assertThat(find.debit(row)).hasText("$" + transfer.amount());
        assertThat(find.credit(row)).hasText("");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Searching by transaction id shows only that transaction")
    void findByIdShowsTransaction(APIRequestContext request) {
        Transfer transfer = transfer(request, customer);

        FindTransactionsPage find = loginAndGoToFindTransactions(customer)
                .selectAccount(String.valueOf(transfer.fromId()))
                .findById(String.valueOf(transfer.sentId()));

        assertThat(find.resultTitle()).hasText("Transaction Results");
        assertThat(find.rows()).hasCount(1);
        assertThat(find.row("Funds Transfer Sent")).hasCount(1);
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Searching by a date range around today shows today's transfer")
    void findByDateRangeShowsTransfer(APIRequestContext request) {
        Transfer transfer = transfer(request, customer);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        FindTransactionsPage find = loginAndGoToFindTransactions(customer)
                .selectAccount(String.valueOf(transfer.fromId()))
                .findByDateRange(today.minusDays(1).format(DATE), today.plusDays(1).format(DATE));

        assertThat(find.resultTitle()).hasText("Transaction Results");
        Locator row = find.row("Funds Transfer Sent").filter(new Locator.FilterOptions().setHasText("$" + transfer.amount()));
        assertThat(row).hasCount(1);
    }

    @ParameterizedTest(name = "Search by {0} with \"{1}\" shows \"{3}\"")
    @CsvSource(delimiter = '|', value = {
            "id     | abc        | transactionIdError   | Invalid transaction ID",
            "amount | abc        | amountError          | Invalid amount",
            "date   | 2026/11/26 | transactionDateError | Invalid date format"})
    @Severity(SeverityLevel.NORMAL)
    void invalidSearchShowsMessage(String search, String value, String messageId, String message) {
        FindTransactionsPage find = loginAndGoToFindTransactions(customer);
        switch (search) {
            case "id" -> find.findById(value);
            case "amount" -> find.findByAmount(value);
            default -> find.findByDate(value);
        }

        assertThat(find.validationMessage(messageId)).hasText(message);
        assertThat(find.resultTitle()).isHidden();
    }

    /**
     * Known defect BUG-014, see docs/defects.md: the search by id does not check the account,
     * so the page shows another customer's transaction.
     * Excluded from the default run; run it with: mvn test -Dgroups=known-defect -DexcludedGroups=
     */
    @Test
    @Tag("known-defect")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("BUG-014 · A transaction of another customer is not shown")
    void otherCustomersTransactionIsNotShown(APIRequestContext request) {
        Customer other = TestUsers.registerNewCustomer();
        Transfer othersTransfer = transfer(request, other);
        String ownAccount = firstAccountId(request, customer);

        FindTransactionsPage find = loginAndGoToFindTransactions(customer)
                .selectAccount(ownAccount)
                .findById(String.valueOf(othersTransfer.sentId()));

        // Either an empty result or an error is acceptable; showing the other customer's transaction is not
        assertThat(page.locator("#resultContainer:visible, #errorContainer:visible").first()).isVisible();
        assertThat(find.rows()).hasCount(0);
    }

    /** Transfer made through the API from the customer's first account to a new account. */
    private record Transfer(long fromId, long sentId, String amount) {
    }

    private Transfer transfer(APIRequestContext request, Customer owner) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(owner.username(), owner.password());
        long fromId = api.accountsOf(apiCustomer.id).get(0).id;
        long toId = api.openAccount(apiCustomer.id, AccountType.CHECKING, fromId).id;
        String amount = TestDataFactory.uniqueAmount();
        api.transfer(fromId, toId, amount);
        TransactionDto sent = api.transactionsOf(fromId).stream()
                .filter(t -> t.description.equals("Funds Transfer Sent") && new BigDecimal(amount).compareTo(t.amount) == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("the API transfer of " + amount + " was not recorded"));
        return new Transfer(fromId, sent.id, amount);
    }

    private String firstAccountId(APIRequestContext request, Customer owner) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(owner.username(), owner.password());
        return String.valueOf(api.accountsOf(apiCustomer.id).get(0).id);
    }

    private FindTransactionsPage loginAndGoToFindTransactions(Customer owner) {
        return new HomePage(page).open()
                .loginAs(owner.username(), owner.password())
                .goToFindTransactions();
    }
}
