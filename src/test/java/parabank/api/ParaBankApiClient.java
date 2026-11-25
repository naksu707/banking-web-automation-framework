package parabank.api;

import com.google.gson.Gson;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

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

    /** Logs in and returns the customer, failing fast if the credentials are rejected. */
    public CustomerDto loginAndGetCustomer(String username, String password) {
        APIResponse response = login(username, password);
        if (response.status() != 200) {
            throw new IllegalStateException("API login failed with status " + response.status() + ": " + response.text());
        }
        return parse(response, CustomerDto.class);
    }

    public List<AccountDto> accountsOf(long customerId) {
        APIResponse response = getAccounts(customerId);
        return Arrays.asList(parse(response, AccountDto[].class));
    }

    public static <T> T parse(APIResponse response, Class<T> type) {
        return GSON.fromJson(response.text(), type);
    }

    private static APIResponse attach(String name, APIResponse response) {
        Allure.addAttachment(name + " → " + response.status(), "application/json", response.text(), ".json");
        return response;
    }
}
