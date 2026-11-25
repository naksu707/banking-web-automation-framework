# Defects

Defects found in ParaBank by this test suite. Tests that expose an open defect are tagged `known-defect` and skipped in the default run, so the pipeline stays green while the defect remains visible and traceable.

Run them with:

```bash
mvn test -Dgroups=known-defect -DexcludedGroups=
```

---

## BUG-001 · Wrong credentials show an internal server error instead of a login error

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Medium: access is correctly denied, but the message is misleading and exposes an internal failure |
| **Priority** | Medium |
| **Area** | Authentication · Login (UI) |
| **Environment** | https://parabank.parasoft.com/parabank/ · Chromium · found 2026-11-24 |
| **Automated test** | `LoginTest.wrongCredentialsShowClearErrorMessage` (tag `known-defect`) |
| **Related test cases** | TC-AUTH-07 |

**Steps to reproduce**

1. Open the ParaBank home page.
2. In *Customer Login*, enter a registered username and a wrong password (or a username that does not exist).
3. Click **Log In**.

**Expected result**

The login is rejected with the message: *"The username and password could not be verified."*

**Actual result**

The login is rejected, but the page shows: *"An internal error has occurred and has been logged."*

**Impact**

- Customers cannot tell whether they mistyped their credentials or the bank is having problems.
- Showing an internal error for a normal user mistake suggests the failed login is handled as an exception, which is worth reviewing from a security and monitoring point of view.

**Notes**

- Access is never granted: the security check itself works and is covered by `LoginTest.invalidLoginIsRejected`.
- Empty credentials show the correct validation message ("Please enter a username and password.").
- The REST API rejects the same credentials correctly with HTTP 400 (`AuthApiTest.invalidLoginIsRejected`).

---

## BUG-002 · The createAccount API response reports a balance of 0 for a funded account

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Low: no money is lost, but the API returns data that does not match the stored account |
| **Priority** | Medium |
| **Area** | Accounts · REST API (`POST /services/bank/createAccount`) |
| **Environment** | https://parabank.parasoft.com/parabank/ · API · found 2026-11-25 |
| **Automated test** | `AccountsApiTest.createAccountResponseShowsStartingBalance` (tag `known-defect`) |
| **Related test cases** | TC-API-ACC-05 |

**Steps to reproduce**

1. Log in through the API to get the customer id and one of their account ids.
2. Send `POST /services/bank/createAccount?customerId={id}&newAccountType=0&fromAccountId={accountId}` with `Accept: application/json`.
3. Read the `balance` field of the response.
4. Send `GET /services/bank/accounts/{newAccountId}` and read its `balance`.

**Expected result**

The creation response shows the account's real starting balance, the same value that `GET /accounts/{id}` returns.

**Actual result**

The creation response shows `"balance": 0`, while `GET /accounts/{id}` returns `100.00`. The source account is debited correctly (for example, from 515.50 to 415.50).

**Impact**

- Any client that trusts the creation response (a mobile app, an integration) shows the customer an empty new account.
- It suggests the response is built before the initial deposit is posted, so other values in the response may also be stale.

**Notes**

- The money itself is consistent: the deposit leaves the source account and arrives in the new one (`AccountsApiTest.initialDepositComesFromSourceAccount` passes).
- The UI shows the correct account, and `OpenAccountAcrossLayersTest` confirms the stored data.

---

## BUG-003 · Wrong credentials open another customer's account

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Critical: authentication bypass and unauthorized access to another customer's data and money |
| **Priority** | Highest |
| **Area** | Authentication · Login (UI) |
| **Environment** | https://parabank.parasoft.com/parabank/ · Chromium · found 2026-11-25 |
| **Automated test** | `LoginTest.invalidLoginIsRejected` (tag `known-defect`) |
| **Related test cases** | TC-AUTH-03, TC-AUTH-04 |
| **Evidence** | [docs/evidence/BUG-003-wrong-credentials-login.png](evidence/BUG-003-wrong-credentials-login.png) |

**Steps to reproduce**

1. Open the ParaBank home page in a new browser session (no cookies).
2. In *Customer Login*, enter a registered username with a wrong password, or a username that does not exist (for example `unknown_user_x9z` / `AnyPass123`).
3. Click **Log In**.

**Expected result**

The login is rejected, no session is opened, and an error message is shown.

**Actual result**

The browser is redirected to `overview.htm` and a session is opened for a different customer: the page shows *"Welcome John Smith"* and the accounts overview of account 13344 (balance $5,022.93). From there, the user has the full menu: transfer funds, bill pay, request loan and update contact info.

**Impact**

- Anyone can enter the online bank without valid credentials.
- The session belongs to a real customer, so an attacker can see their data and move their money.
- Failed logins are not reported, so the customer and the bank have no way to notice the access.

**Notes**

- Reproduced in three full runs on 2026-11-25 (12:21, 12:24 and 12:35).
- **Intermittent:** in the 12:35 run, the same wrong credentials first returned the internal error page (BUG-001) and, about 30 seconds later, opened John Smith's account. The two defects are likely two outcomes of the same faulty error handling in the login.
- The REST API rejects the same credentials correctly with HTTP 400 (`AuthApiTest.invalidLoginIsRejected`): the defect is in the web login only.
- Empty credentials are still rejected correctly (`LoginTest.emptyCredentialsAreRejected`).
- The evidence screenshot is saved automatically by `EvidenceExtension` when the test fails.
