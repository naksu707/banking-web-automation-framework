package parabank.tests.api;

import com.microsoft.playwright.APIResponse;
import parabank.api.AccountDto;
import parabank.api.CustomerDto;
import parabank.api.ParaBankApiClient;
import parabank.utils.BaseApiTest;
import parabank.data.Customer;
import parabank.data.TestUsers;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Epic("Authentication")
@Feature("Login API")
@DisplayName("Login API")
class AuthApiTest extends BaseApiTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Valid credentials return the customer's data")
    void validLoginReturnsCustomer() {
        APIResponse response = api.login(customer.username(), customer.password());
        assertEquals(200, response.status(), "status code");

        CustomerDto body = ParaBankApiClient.parse(response, CustomerDto.class);
        assertAll(
                () -> assertTrue(body.id > 0, "customer id should be positive"),
                () -> assertEquals(customer.firstName(), body.firstName, "first name"),
                () -> assertEquals(customer.lastName(), body.lastName, "last name"),
                () -> assertEquals(customer.city(), body.address.city, "city"));
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Invalid credentials are rejected with HTTP 400")
    void invalidLoginIsRejected() {
        APIResponse response = api.login(customer.username(), "WrongPass123");

        assertEquals(400, response.status(), "status code");
        assertTrue(response.text().contains("Invalid username and/or password"),
                "error message, got: " + response.text());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("A new customer has at least one account, and every account belongs to them")
    void newCustomerHasAccounts() {
        CustomerDto body = api.loginAndGetCustomer(customer.username(), customer.password());

        List<AccountDto> accounts = api.accountsOf(body.id);

        assertFalse(accounts.isEmpty(), "the new customer should have an account");
        accounts.forEach(account -> assertEquals(body.id, account.customerId,
                "account " + account.id + " belongs to another customer"));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("The customer endpoint returns the same data as the login endpoint")
    void customerEndpointMatchesLogin() {
        CustomerDto fromLogin = api.loginAndGetCustomer(customer.username(), customer.password());

        APIResponse response = api.getCustomer(fromLogin.id);
        assertEquals(200, response.status(), "status code");

        CustomerDto fromCustomer = ParaBankApiClient.parse(response, CustomerDto.class);
        assertAll(
                () -> assertEquals(fromLogin.id, fromCustomer.id, "id"),
                () -> assertEquals(fromLogin.firstName, fromCustomer.firstName, "first name"),
                () -> assertEquals(fromLogin.lastName, fromCustomer.lastName, "last name"));
    }
}
