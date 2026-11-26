package parabank.api;

import com.google.gson.Gson;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import io.qameta.allure.Allure;
import com.microsoft.playwright.options.RequestOptions;
import io.qameta.allure.Step;
import parabank.data.AccountType;
import parabank.data.Payee;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Client for the ParaBank REST API (services/bank/...), built on Playwright's APIRequestContext.
 * Every call attaches the response to the Allure report.
 */
public class ParaBankApiClient {

    private static final Gson GSON = new Gson();
    private static final String BASE = "services/bank/";

    private final APIRequestContext request;

    public ParaBankApiClient(APIRequestContext request) {
        this.request = request;
    }

    @Step("API: log in as {username}")
    public APIResponse login(String username, String password) {
        return attach("GET login", request.get(BASE + "login/" + username + "/" + password));
    }

    @Step("API: get customer {customerId}")
    public APIResponse getCustomer(long customerId) {
        return attach("GET customer", request.get(BASE + "customers/" + customerId));
    }

    @Step("API: get accounts of customer {customerId}")
    public APIResponse getAccounts(long customerId) {
        return attach("GET accounts", request.get(BASE + "customers/" + customerId + "/accounts"));
    }

    @Step("API: get account {accountId}")
    public APIResponse getAccount(long accountId) {
        return attach("GET account", request.get(BASE + "accounts/" + accountId));
    }

    @Step("API: open a new {type} account for customer {customerId}, funded from account {fromAccountId}")
    public APIResponse createAccount(long customerId, AccountType type, long fromAccountId) {
        return attach("POST createAccount", request.post(BASE + "createAccount", RequestOptions.create()
                .setQueryParam("customerId", String.valueOf(customerId))
                .setQueryParam("newAccountType", String.valueOf(type.code()))
                .setQueryParam("fromAccountId", String.valueOf(fromAccountId))));
    }

    @Step("API: transfer {amount} from account {fromAccountId} to account {toAccountId}")
    public APIResponse transfer(long fromAccountId, long toAccountId, String amount) {
        return attach("POST transfer", request.post(BASE + "transfer", RequestOptions.create()
                .setQueryParam("fromAccountId", String.valueOf(fromAccountId))
                .setQueryParam("toAccountId", String.valueOf(toAccountId))
                .setQueryParam("amount", amount)));
    }

    @Step("API: pay {amount} to {payee.name} from account {accountId}")
    public APIResponse billPay(long accountId, String amount, Payee payee) {
        Map<String, Object> body = Map.of(
                "name", payee.name(),
                "address", Map.of(
                        "street", payee.street(),
                        "city", payee.city(),
                        "state", payee.state(),
                        "zipCode", payee.zipCode()),
                "phoneNumber", payee.phoneNumber(),
                "accountNumber", Long.parseLong(payee.accountNumber()));
        return attach("POST billpay", request.post(BASE + "billpay", RequestOptions.create()
                .setQueryParam("accountId", String.valueOf(accountId))
                .setQueryParam("amount", amount)
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .setData(GSON.toJson(body))));
    }

    /** Logs in and returns the customer, failing fast if the credentials are rejected. */
    public CustomerDto loginAndGetCustomer(String username, String password) {
        APIResponse response = login(username, password);
        if (response.status() == 429) {
            expectOk(response);
        }
        if (response.status() != 200) {
            throw new IllegalStateException("API login failed with status " + response.status() + ": " + response.text());
        }
        return parse(response, CustomerDto.class);
    }

    public List<AccountDto> accountsOf(long customerId) {
        return Arrays.asList(parse(expectOk(getAccounts(customerId)), AccountDto[].class));
    }

    public AccountDto account(long accountId) {
        return parse(expectOk(getAccount(accountId)), AccountDto.class);
    }

    /** Opens an account through the API, failing fast if ParaBank rejects it. */
    public AccountDto openAccount(long customerId, AccountType type, long fromAccountId) {
        return parse(expectOk(createAccount(customerId, type, fromAccountId)), AccountDto.class);
    }

    public java.math.BigDecimal balanceOf(long accountId) {
        return account(accountId).balance;
    }

    /**
     * Reads the balance until it equals the expected value, for up to 15 seconds.
     * ParaBank sometimes takes a moment to show a change made in the UI. Returns the last balance read.
     */
    public java.math.BigDecimal awaitBalance(long accountId, java.math.BigDecimal expected) {
        java.math.BigDecimal balance = balanceOf(accountId);
        for (int i = 0; i < 30 && expected.compareTo(balance) != 0; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            balance = balanceOf(accountId);
        }
        return balance;
    }

    public static <T> T parse(APIResponse response, Class<T> type) {
        return GSON.fromJson(response.text(), type);
    }

    private static APIResponse expectOk(APIResponse response) {
        if (response.status() == 429) {
            throw new IllegalStateException("ParaBank is rate limiting this IP (HTTP 429 from Cloudflare). "
                    + "Wait 15-30 minutes before running the tests again.");
        }
        if (response.status() != 200) {
            throw new IllegalStateException("Unexpected API status " + response.status() + ": " + response.text());
        }
        return response;
    }

    private static APIResponse attach(String name, APIResponse response) {
        Allure.addAttachment(name + " → " + response.status(), "application/json", response.text(), ".json");
        return response;
    }
}
