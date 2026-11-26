# Test Cases

Traceability between the test cases and their automated tests.

**Priority** follows the risk-based strategy in the main README: Critical = blocks access to money or the account, High = core feature, Medium = supporting behavior.

## Authentication · UI

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-AUTH-01 | A registered customer logs in and sees the accounts overview | Critical | Smoke, positive | `LoginTest.validLoginOpensAccountsOverview` |
| TC-AUTH-02 | The welcome message shows the customer's full name | Medium | Positive | `LoginTest.welcomeMessageShowsCustomerName` |
| TC-AUTH-03 | Login with a wrong password does not give access | Critical | Negative, security | `LoginTest.invalidLoginIsRejected` (wrong password) · ❌ fails: [BUG-003](defects.md#bug-003--wrong-credentials-open-another-customers-account) |
| TC-AUTH-04 | Login with an unknown username does not give access | Critical | Negative, security | `LoginTest.invalidLoginIsRejected` (unknown username) · ❌ fails: [BUG-003](defects.md#bug-003--wrong-credentials-open-another-customers-account) |
| TC-AUTH-05 | Login with empty fields does not give access and shows a validation message | High | Negative | `LoginTest.emptyCredentialsAreRejected`, `LoginTest.emptyCredentialsShowValidationMessage` |
| TC-AUTH-06 | Logging out ends the session and shows the login form | Critical | Smoke | `LogoutTest.logoutReturnsToLoginForm` |
| TC-AUTH-07 | Wrong credentials show a clear error message | Medium | Negative | `LoginTest.wrongCredentialsShowClearErrorMessage` · ❌ fails: [BUG-001](defects.md#bug-001--wrong-credentials-show-an-internal-server-error-instead-of-a-login-error) |
| TC-REG-01 | A new customer registers and is logged in automatically | Critical | Smoke, positive | `RegistrationTest.newCustomerCanRegister` |
| TC-REG-02 | An existing username cannot be registered again | Critical | Negative | `RegistrationTest.duplicateUsernameIsRejected` |
| TC-REG-03 | Registration fails when the password confirmation does not match | High | Negative | `RegistrationTest.passwordConfirmationMustMatch` |
| TC-REG-04 | An empty form shows a message for every required field | High | Negative | `RegistrationTest.requiredFieldsAreValidated` |

## Authentication · API

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-API-AUTH-01 | `GET /login/{user}/{password}` with valid credentials returns 200 and the customer | Critical | Smoke, positive | `AuthApiTest.validLoginReturnsCustomer` |
| TC-API-AUTH-02 | Invalid credentials return 400 with an error message | Critical | Smoke, negative | `AuthApiTest.invalidLoginIsRejected` |
| TC-API-AUTH-03 | A new customer has at least one account, and all accounts belong to them | Critical | Positive | `AuthApiTest.newCustomerHasAccounts` |
| TC-API-AUTH-04 | `GET /customers/{id}` returns the same data as the login endpoint | Medium | Positive | `AuthApiTest.customerEndpointMatchesLogin` |

## Integration · UI vs. API

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-INT-01 | The accounts overview lists the same accounts returned by the API | Critical | Integration | `AccountsAcrossLayersTest.overviewMatchesApiAccounts` |

## Accounts · UI

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-ACC-01 | A customer opens a new checking account and sees the confirmation with the new account number | Critical | Smoke, positive | `OpenAccountTest.newAccountIsOpened` (CHECKING) |
| TC-ACC-02 | A customer opens a new savings account and sees the confirmation with the new account number | Critical | Smoke, positive | `OpenAccountTest.newAccountIsOpened` (SAVINGS) |
| TC-ACC-03 | The new account's details page shows its number and the chosen type | Medium | Positive | `OpenAccountTest.newAccountDetailsShowChosenType` |
| TC-ACC-04 | The new account appears in the accounts overview | Critical | Positive | `OpenAccountTest.newAccountAppearsInOverview` |

## Accounts · API

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-API-ACC-01 | `POST /createAccount` returns 200 and a new account of the requested type, owned by the customer | Critical | Smoke, positive | `AccountsApiTest.createAccountReturnsNewAccount` |
| TC-API-ACC-02 | A new account is listed in `GET /customers/{id}/accounts` | Critical | Positive | `AccountsApiTest.newAccountIsListed` |
| TC-API-ACC-03 | `GET /accounts/{id}` returns the same id, owner and type as the account creation | Medium | Positive | `AccountsApiTest.getAccountMatchesCreatedAccount` |
| TC-API-ACC-04 | The initial deposit is debited from the source account: source before − deposit = source after | Critical | Positive, money integrity | `AccountsApiTest.initialDepositComesFromSourceAccount` |
| TC-API-ACC-05 | The `POST /createAccount` response shows the account's real starting balance | Medium | Positive | `AccountsApiTest.createAccountResponseShowsStartingBalance` · ❌ fails: [BUG-002](defects.md#bug-002--the-createaccount-api-response-reports-a-balance-of-0-for-a-funded-account) |

## Integration · Open account

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-INT-02 | An account opened in the UI exists in the API with the right owner, type and funding | Critical | Integration | `OpenAccountAcrossLayersTest.accountOpenedInUiIsStoredCorrectly` |

## Transfers · UI

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-TRF-01 | A transfer between own accounts shows "Transfer Complete!" with the amount and both account numbers | Critical | Smoke, positive | `TransferFundsTest.transferShowsConfirmation` |
| TC-TRF-02 | An empty amount is rejected and no money moves | Critical | Negative | `TransferFundsTest.invalidAmountIsRejected` ("") |
| TC-TRF-03 | A negative amount is rejected and no money moves | Critical | Negative | `TransferFundsTest.negativeOrZeroAmountIsRejected` ("-50") · ❌ fails: [BUG-004](defects.md#bug-004--transfers-accept-a-negative-amount-which-moves-money-out-of-the-destination-account) |
| TC-TRF-04 | A zero amount is rejected and no money moves | High | Negative | `TransferFundsTest.negativeOrZeroAmountIsRejected` ("0") · ❌ fails: [BUG-006](defects.md#bug-006--transfers-accept-an-amount-of-zero) |
| TC-TRF-05 | A non-numeric amount is rejected and no money moves | High | Negative | `TransferFundsTest.invalidAmountIsRejected` ("abc") |

## Transfers · API

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-API-TRF-01 | `POST /transfer` between own accounts returns 200 and a confirmation | Critical | Smoke, positive | `TransferApiTest.transferSucceeds` |
| TC-API-TRF-02 | A transfer moves exactly the amount: source − amount, destination + amount | Critical | Positive, money integrity | `TransferApiTest.transferMovesExactAmount` |
| TC-API-TRF-03 | A negative amount returns 400 and no money moves | Critical | Negative | `TransferApiTest.negativeAmountIsRejected` · ❌ fails: [BUG-004](defects.md#bug-004--transfers-accept-a-negative-amount-which-moves-money-out-of-the-destination-account) |
| TC-API-TRF-04 | A zero amount returns 400 and no money moves | High | Negative | `TransferApiTest.zeroAmountIsRejected` · ❌ fails: [BUG-006](defects.md#bug-006--transfers-accept-an-amount-of-zero) |
| TC-API-TRF-05 | An amount greater than the source balance returns 400 and no money moves | Critical | Negative | `TransferApiTest.amountAboveBalanceIsRejected` · ❌ fails: [BUG-005](defects.md#bug-005--transfers-do-not-check-the-available-balance-and-leave-the-account-overdrawn) |
| TC-API-TRF-06 | A transfer to the same account returns 400 | Medium | Negative | `TransferApiTest.sameAccountTransferIsRejected` · ❌ fails: [BUG-007](defects.md#bug-007--a-transfer-to-the-same-account-is-accepted) |

## Integration · Transfers

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-INT-03 | A transfer made in the UI moves exactly the amount between the two accounts in the back end | Critical | Integration, money integrity | `TransferAcrossLayersTest.uiTransferUpdatesBothBalances` |

## Bill pay · UI

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-BILL-01 | A bill payment shows "Bill Payment Complete" with the payee, the amount and the account | Critical | Smoke, positive | `BillPayTest.billPaymentShowsConfirmation` |
| TC-BILL-02 | An empty form shows a required-field message for every field | Medium | Negative, validation | `BillPayTest.emptyFormShowsRequiredMessages` |
| TC-BILL-03 | Different payee and verify account numbers are rejected and no money leaves the account | Critical | Negative | `BillPayTest.accountMismatchIsRejected` |
| TC-BILL-04 | An empty amount shows "The amount cannot be empty" and no money leaves the account | High | Negative | `BillPayTest.invalidAmountIsRejected` ("") |
| TC-BILL-05 | A non-numeric amount shows "Please enter a valid amount" and no money leaves the account | High | Negative | `BillPayTest.invalidAmountIsRejected` ("abc") |
| TC-BILL-06 | A negative amount is rejected and no money leaves the account | Critical | Negative | `BillPayTest.negativeOrZeroAmountIsRejected` ("-20") · ❌ fails: [BUG-008](defects.md#bug-008--bill-payments-accept-a-negative-amount-which-adds-money-to-the-account) |
| TC-BILL-07 | A zero amount is rejected | High | Negative | `BillPayTest.negativeOrZeroAmountIsRejected` ("0") · ❌ fails: [BUG-010](defects.md#bug-010--bill-payments-accept-an-amount-of-zero) |

## Bill pay · API

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-API-BILL-01 | `POST /billpay` returns 200 with the payee name, the amount and the account | Critical | Smoke, positive | `BillPayApiTest.billPaySucceeds` |
| TC-API-BILL-02 | A bill payment takes exactly the amount out of the account | Critical | Positive, money integrity | `BillPayApiTest.billPayDebitsExactAmount` |
| TC-API-BILL-03 | A negative amount returns 400 and no money moves | Critical | Negative | `BillPayApiTest.negativeAmountIsRejected` · ❌ fails: [BUG-008](defects.md#bug-008--bill-payments-accept-a-negative-amount-which-adds-money-to-the-account) |
| TC-API-BILL-04 | An amount greater than the balance returns 400 and no money moves | Critical | Negative | `BillPayApiTest.amountAboveBalanceIsRejected` · ❌ fails: [BUG-009](defects.md#bug-009--bill-payments-do-not-check-the-available-balance-and-leave-the-account-overdrawn) |

## Integration · Bill pay

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-INT-04 | A bill paid in the UI takes exactly the amount out of the account in the back end | Critical | Integration, money integrity | `BillPayAcrossLayersTest.uiBillPaymentDebitsAccount` · ❌ fails intermittently: [BUG-011](defects.md#bug-011--a-confirmed-payment-or-transfer-sometimes-takes-no-money-out-of-the-source-account) |
