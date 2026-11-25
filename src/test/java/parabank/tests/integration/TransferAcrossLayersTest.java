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
import parabank.pages.TransferFundsPage;
import parabank.utils.BaseTest;

import java.math.BigDecimal;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Makes a transfer in the UI and checks both balances in the back end through the API. */
@Tag("integration")
@Epic("Transfers")
@Feature("UI and API consistency")
@DisplayName("Transfer funds: UI vs. API")
class TransferAcrossLayersTest extends BaseTest {

    private static final BigDecimal AMOUNT = new BigDecimal("33.50");

    private static Customer customer;

    @BeforeAll
    static void registerCustomer() {
        customer = TestUsers.registerNewCustomer();
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("A transfer made in the UI moves exactly the amount between the two accounts")
    void uiTransferUpdatesBothBalances(APIRequestContext request) {
        ParaBankApiClient api = new ParaBankApiClient(request);
        CustomerDto apiCustomer = api.loginAndGetCustomer(customer.username(), customer.password());
        long fromId = api.accountsOf(apiCustomer.id).get(0).id;
        AccountDto to = api.openAccount(apiCustomer.id, AccountType.SAVINGS, fromId);
        BigDecimal fromBefore = api.balanceOf(fromId);
        BigDecimal toBefore = api.balanceOf(to.id);

        TransferFundsPage transfer = new HomePage(page).open()
                .loginAs(customer.username(), customer.password())
                .goToTransferFunds()
                .transfer(AMOUNT.toPlainString(), String.valueOf(fromId), String.valueOf(to.id));
        assertThat(transfer.resultTitle()).hasText("Transfer Complete!");
        // The confirmation must name the accounts that were chosen, so the page did not swap them
        assertThat(transfer.fromAccountResult()).hasText(String.valueOf(fromId));
        assertThat(transfer.toAccountResult()).hasText(String.valueOf(to.id));

        // Give the back end a few seconds, so a slow update is not reported as a defect
        BigDecimal expectedFrom = fromBefore.subtract(AMOUNT);
        BigDecimal fromAfter = api.balanceOf(fromId);
        for (int i = 0; i < 10 && expectedFrom.compareTo(fromAfter) != 0; i++) {
            page.waitForTimeout(500);
            fromAfter = api.balanceOf(fromId);
        }
        BigDecimal toAfter = api.balanceOf(to.id);
        BigDecimal finalFromAfter = fromAfter;
        assertAll(
                () -> assertEquals(0, expectedFrom.compareTo(finalFromAfter),
                        "source: before " + fromBefore + ", after " + finalFromAfter),
                () -> assertEquals(0, toBefore.add(AMOUNT).compareTo(toAfter),
                        "destination: before " + toBefore + ", after " + toAfter),
                () -> assertEquals(0, fromBefore.add(toBefore).compareTo(finalFromAfter.add(toAfter)),
                        "total across both accounts changed: before " + fromBefore.add(toBefore)
                                + ", after " + finalFromAfter.add(toAfter)));
    }
}
