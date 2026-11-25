package parabank.api;

import com.google.gson.Gson;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import io.qameta.allure.Allure;
import com.microsoft.playwright.options.RequestOptions;
import io.qameta.allure.Step;
import parabank.data.AccountType;

import java.util.Arrays;
import java.util.List;

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
