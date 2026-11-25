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

---

## BUG-004 · Transfers accept a negative amount, which moves money out of the destination account

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Critical: a customer can pull money out of any account they can name as the destination |
| **Priority** | Highest |
| **Area** | Transfers · REST API (`POST /services/bank/transfer`) and UI (Transfer Funds) |
| **Environment** | https://parabank.parasoft.com/parabank/ · API and Chromium · found 2026-11-25 |
| **Automated tests** | `TransferApiTest.negativeAmountIsRejected`, `TransferFundsTest.negativeOrZeroAmountIsRejected` ("-50") (tag `known-defect`) |
| **Related test cases** | TC-API-TRF-03, TC-TRF-03 |

**Steps to reproduce**

1. Log in and note the balances of two accounts, A (source) and B (destination).
2. Send `POST /services/bank/transfer?fromAccountId={A}&toAccountId={B}&amount=-40.00` (or enter `-50` in *Transfer Funds*).

**Expected result**

The transfer is rejected (HTTP 400 / validation message) and both balances stay the same.

**Actual result**

The API answers `200 Successfully transferred $-40.00 from account #A to account #B`. Account A goes **up** from 415.50 to 455.50 and account B goes **down** from 100.00 to 60.00. The UI shows *"Transfer Complete!"*.

**Impact**

- The transfer runs in reverse: the "source" customer takes money from the "destination" account without its owner's consent.
- Combined with an account number of another customer, this becomes a way to steal funds.

---

## BUG-005 · Transfers do not check the available balance and leave the account overdrawn

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Critical: money can be transferred that the customer does not have |
| **Priority** | Highest |
| **Area** | Transfers · REST API (`POST /services/bank/transfer`) |
| **Environment** | https://parabank.parasoft.com/parabank/ · API · found 2026-11-25 |
| **Automated test** | `TransferApiTest.amountAboveBalanceIsRejected` (tag `known-defect`) |
| **Related test cases** | TC-API-TRF-05 |

**Steps to reproduce**

1. Log in and note the balance of account A (for example, 355.50).
2. Send `POST /services/bank/transfer?fromAccountId={A}&toAccountId={B}&amount=1355.50` (balance + 1,000).

**Expected result**

The transfer is rejected for insufficient funds and both balances stay the same.

**Actual result**

The API answers `200 Successfully transferred $1355.50`. Account A ends at **-1,000.00** and account B receives the full amount.

**Impact**

- Unlimited overdraft: customers can move money they do not have to other accounts.
- Breaks the basic rule of any account without an approved credit line.

---

## BUG-006 · Transfers accept an amount of zero

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Medium: no money moves, but an empty transaction is confirmed and recorded |
| **Priority** | Medium |
| **Area** | Transfers · UI (Transfer Funds) and REST API |
| **Environment** | https://parabank.parasoft.com/parabank/ · Chromium · found 2026-11-25 |
| **Automated tests** | `TransferFundsTest.negativeOrZeroAmountIsRejected` ("0"), `TransferApiTest.zeroAmountIsRejected` (tag `known-defect`) |
| **Related test cases** | TC-TRF-04, TC-API-TRF-04 |

**Steps to reproduce**

1. Log in and open *Transfer Funds*.
2. Enter `0` as the amount, choose two accounts and click **Transfer**.

**Expected result**

A validation message asks for an amount greater than zero.

**Actual result**

The page shows *"Transfer Complete!"* for $0.00.

**Notes**

- The API behavior for a zero amount was not observed yet: the run was rate limited (HTTP 429) before that test. It is expected to match the UI, since the UI calls the same service.

---

## BUG-007 · A transfer to the same account is accepted

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Low: no money moves, but a meaningless transaction is confirmed and recorded |
| **Priority** | Low |
| **Area** | Transfers · REST API (`POST /services/bank/transfer`) |
| **Environment** | https://parabank.parasoft.com/parabank/ · API · found 2026-11-25 |
| **Automated test** | `TransferApiTest.sameAccountTransferIsRejected` (tag `known-defect`) |
| **Related test cases** | TC-API-TRF-06 |

**Steps to reproduce**

1. Send `POST /services/bank/transfer?fromAccountId={A}&toAccountId={A}&amount=40.00`.

**Expected result**

The transfer is rejected because the source and destination are the same account.

**Actual result**

The API answers `200 Successfully transferred $40.00 from account #A to account #A`.
