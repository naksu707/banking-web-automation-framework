# Test Cases

Traceability between the test cases and their automated tests.

**Priority** follows the risk-based strategy in the main README: Critical = blocks access to money or the account, High = core feature, Medium = supporting behavior.

## Authentication · UI

| ID | Test case | Priority | Type | Automated test |
|---|---|---|---|---|
| TC-AUTH-01 | A registered customer logs in and sees the accounts overview | Critical | Smoke, positive | `LoginTest.validLoginOpensAccountsOverview` |
| TC-AUTH-02 | The welcome message shows the customer's full name | Medium | Positive | `LoginTest.welcomeMessageShowsCustomerName` |
| TC-AUTH-03 | Login with a wrong password does not give access | Critical | Smoke, negative | `LoginTest.invalidLoginIsRejected` (wrong password) |
| TC-AUTH-04 | Login with an unknown username does not give access | Critical | Smoke, negative | `LoginTest.invalidLoginIsRejected` (unknown username) |
| TC-AUTH-05 | Login with empty fields does not give access and shows a validation message | High | Negative | `LoginTest.invalidLoginIsRejected` (empty fields), `LoginTest.emptyCredentialsShowValidationMessage` |
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
