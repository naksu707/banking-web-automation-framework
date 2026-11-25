package parabank.tests.ui;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import parabank.data.AccountType;
import parabank.data.Customer;
import parabank.data.TestUsers;
import parabank.pages.AccountActivityPage;
import parabank.pages.AccountsOverviewPage;
import parabank.pages.HomePage;
import parabank.pages.OpenAccountPage;
import parabank.utils.BaseTest;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Epic("Accounts")
@Feature("Open new account")
@DisplayName("Open new account")
class OpenAccountTest extends BaseTest {

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @ParameterizedTest(name = "A new {0} account can be opened")
    @EnumSource(AccountType.class)
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    void newAccountIsOpened(AccountType type) {
        OpenAccountPage openAccount = loginAndGoToOpenAccount().openAccount(type);

        assertThat(openAccount.resultTitle()).hasText("Account Opened!");
        assertThat(openAccount.resultMessage()).hasText("Congratulations, your account is now open.");
        assertThat(openAccount.newAccountId()).hasText(Pattern.compile("^\\d+$"));
    }

    @Test
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("The new account's details show its number and the chosen type")
    void newAccountDetailsShowChosenType() {
        OpenAccountPage openAccount = loginAndGoToOpenAccount().openAccount(AccountType.SAVINGS);
        String accountNumber = openAccount.newAccountNumber();

        AccountActivityPage details = openAccount.openNewAccountDetails();

        assertThat(details.accountId()).hasText(accountNumber);
        assertThat(details.accountType()).hasText(AccountType.SAVINGS.name());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("The new account appears in the accounts overview")
    void newAccountAppearsInOverview() {
        OpenAccountPage openAccount = loginAndGoToOpenAccount().openAccount(AccountType.CHECKING);
        String accountNumber = openAccount.newAccountNumber();

        AccountsOverviewPage overview = openAccount.goToAccountsOverview();

        assertThat(overview.accountLink(accountNumber)).isVisible();
    }

    private OpenAccountPage loginAndGoToOpenAccount() {
        return new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToOpenNewAccount();
    }
}
