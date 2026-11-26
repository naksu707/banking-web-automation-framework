package parabank.tests.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import parabank.utils.BaseApiTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One transfer with a unique amount is made once for the whole class, so its transactions are easy to find
 * and the class sends few requests (ParaBank rate limits busy IPs).
 */
@Epic("Transactions")
@Feature("Transactions API")
@DisplayName("Transactions API")
class TransactionsApiTest extends BaseApiTest {

    /** Format of the dates in the search endpoints. */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private static Customer customer;
    private static long fromId;
    private static long toId;
    private static String amount;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    /** Runs once: the API client is per test, so the transfer is made in the first test's set-up. */
    @BeforeEach
    void transferOnce() {
        if (amount != null) {
            return;
        }
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        fromId = api.accountsOf(apiCustomer.id).get(0).id;
        toId = api.openAccount(apiCustomer.id, AccountType.CHECKING, fromId).id;
        String value = TestDataFactory.uniqueAmount();
        assertEquals(200, api.transfer(fromId, toId, value).status(), "transfer used to create the transactions");
        amount = value;
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A transfer is recorded as a debit in the source and a credit in the destination")
    void transferIsRecordedInBothAccounts() {
        TransactionDto sent = find(api.transactionsOf(fromId), "Funds Transfer Sent");
        TransactionDto received = find(api.transactionsOf(toId), "Funds Transfer Received");

        assertAll(
                () -> assertEquals("Debit", sent.type, "source type"),
                () -> assertEquals(fromId, sent.accountId, "source account"),
                () -> assertEquals("Credit", received.type, "destination type"),
                () -> assertEquals(toId, received.accountId, "destination account"));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("A bill payment is recorded as a debit with the payee name")
    void billPaymentIsRecorded() {
        Payee payee = TestDataFactory.newPayee();
        String billAmount = TestDataFactory.uniqueAmount();
        assertEquals(200, api.billPay(fromId, billAmount, payee).status(), "bill payment");

        TransactionDto payment = find(api.transactionsOf(fromId), "Bill Payment to " + payee.name(), billAmount);

        assertEquals("Debit", payment.type, "type");
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Searching by amount returns only transactions of that amount")
    void findByAmountReturnsOnlyThatAmount() {
        List<TransactionDto> found = list(api.findTransactionsByAmount(fromId, amount));

        assertFalse(found.isEmpty(), "the transfer of " + amount + " was not found");
        for (TransactionDto transaction : found) {
            assertEquals(0, new BigDecimal(amount).compareTo(transaction.amount), "amount of transaction " + transaction.id);
            assertEquals(fromId, transaction.accountId, "account of transaction " + transaction.id);
        }
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("A transaction read by id matches the one in the account history")
    void transactionByIdMatchesHistory() {
        TransactionDto inHistory = find(api.transactionsOf(fromId), "Funds Transfer Sent");

        TransactionDto byId = ParaBankApiClient.parse(api.getTransaction(inHistory.id), TransactionDto.class);

        assertAll(
                () -> assertEquals(inHistory.id, byId.id, "id"),
                () -> assertEquals(inHistory.accountId, byId.accountId, "account"),
                () -> assertEquals(inHistory.type, byId.type, "type"),
                () -> assertEquals(0, inHistory.amount.compareTo(byId.amount), "amount"),
                () -> assertEquals(inHistory.description, byId.description, "description"));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("Searching by a date range around today finds today's transfer")
    void findByDateRangeIncludesTransfer() {
        // One day on each side, so the server's time zone does not matter
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<TransactionDto> found = list(api.findTransactionsByDateRange(
                fromId, today.minusDays(1).format(DATE), today.plusDays(1).format(DATE)));

        assertTrue(found.stream().anyMatch(t -> t.description.equals("Funds Transfer Sent")
                        && new BigDecimal(amount).compareTo(t.amount) == 0),
                "the transfer of " + amount + " is missing from " + found.size() + " transactions");
    }

    private List<TransactionDto> list(com.microsoft.playwright.APIResponse response) {
        assertEquals(200, response.status(), "status code, body: " + response.text());
        return Arrays.asList(ParaBankApiClient.parse(response, TransactionDto[].class));
    }

    private TransactionDto find(List<TransactionDto> transactions, String description) {
        return find(transactions, description, amount);
    }

    private TransactionDto find(List<TransactionDto> transactions, String description, String value) {
        return transactions.stream()
                .filter(t -> t.description.equals(description) && new BigDecimal(value).compareTo(t.amount) == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no \"" + description + "\" of " + value
                        + " among " + transactions.size() + " transactions"));
    }
}
