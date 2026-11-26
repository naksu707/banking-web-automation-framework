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

---

## BUG-008 · Bill payments accept a negative amount, which adds money to the account

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Critical: a customer can increase their own balance by "paying" a negative bill |
| **Priority** | Highest |
| **Area** | Bill pay · REST API (`POST /services/bank/billpay`) and UI (Bill Pay) |
| **Environment** | https://parabank.parasoft.com/parabank/ · API and Chromium · found 2026-11-25 |
| **Automated tests** | `BillPayApiTest.negativeAmountIsRejected`, `BillPayTest.negativeOrZeroAmountIsRejected` ("-20") (tag `known-defect`) |
| **Related test cases** | TC-API-BILL-03, TC-BILL-06 |
| **Related defect** | [BUG-004](#bug-004--transfers-accept-a-negative-amount-which-moves-money-out-of-the-destination-account) (same missing validation in transfers) |

**Steps to reproduce**

1. Log in and note the balance of account A (for example, 515.50).
2. Send `POST /services/bank/billpay?accountId={A}&amount=-60.00` with a valid payee in the JSON body (or enter `-20` as the amount in *Bill Pay*).

**Expected result**

The payment is rejected (HTTP 400 / validation message) and the balance stays the same.

**Actual result**

The API answers `200 {"payeeName":"City Power","amount":-60.00,"accountId":15564}` and the balance goes **up** from 515.50 to 575.50. The UI shows *"Bill Payment Complete"*.

**Impact**

- Customers can create money in their own account, with no limit and no approval.
- The payee is recorded as having been paid a negative amount, which corrupts the transaction history.

---

## BUG-009 · Bill payments do not check the available balance and leave the account overdrawn

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Critical: bills can be paid with money the customer does not have |
| **Priority** | Highest |
| **Area** | Bill pay · REST API (`POST /services/bank/billpay`) |
| **Environment** | https://parabank.parasoft.com/parabank/ · API · found 2026-11-25 |
| **Automated test** | `BillPayApiTest.amountAboveBalanceIsRejected` (tag `known-defect`) |
| **Related test cases** | TC-API-BILL-04 |
| **Related defect** | [BUG-005](#bug-005--transfers-do-not-check-the-available-balance-and-leave-the-account-overdrawn) (same missing check in transfers) |

**Steps to reproduce**

1. Log in and note the balance of account A (for example, 455.50).
2. Send `POST /services/bank/billpay?accountId={A}&amount=1455.50` (balance + 1,000) with a valid payee.

**Expected result**

The payment is rejected for insufficient funds and the balance stays the same.

**Actual result**

The API answers `200 {"payeeName":"Water Utility","amount":1455.50,...}` and the balance ends at **-1,000.00**.

**Impact**

- Unlimited overdraft through bill payments, without an approved credit line.

---

## BUG-010 · Bill payments accept an amount of zero

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Medium: no money moves, but an empty payment is confirmed and recorded |
| **Priority** | Medium |
| **Area** | Bill pay · UI (Bill Pay) |
| **Environment** | https://parabank.parasoft.com/parabank/ · Chromium · found 2026-11-25 |
| **Automated test** | `BillPayTest.negativeOrZeroAmountIsRejected` ("0") (tag `known-defect`) |
| **Related test cases** | TC-BILL-07 |
| **Related defect** | [BUG-006](#bug-006--transfers-accept-an-amount-of-zero) (same missing validation in transfers) |

**Steps to reproduce**

1. Log in and open *Bill Pay*.
2. Fill in a valid payee, enter `0` as the amount and click **Send Payment**.

**Expected result**

A validation message asks for an amount greater than zero.

**Actual result**

The page shows *"Bill Payment Complete"* for $0.00.

**Notes**

- The form only checks that the amount is a number (`parseFloat`), so any negative or zero value passes the browser validation and reaches the service, which does not validate it either.

---

## BUG-011 · A confirmed payment or transfer sometimes takes no money out of the source account

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Critical: the customer is told the money was sent, but the source account keeps it |
| **Priority** | Highest |
| **Area** | Bill pay and Transfer Funds · UI (requests sent by the browser through `services_proxy`) |
| **Environment** | https://parabank.parasoft.com/parabank/ · Chromium · found 2026-11-25 |
| **Reproducibility** | Intermittent: about half of the runs on 2026-11-25 |
| **Automated test** | `BillPayAcrossLayersTest.uiBillPaymentDebitsAccount` (tag `known-defect`) |
| **Related test cases** | TC-INT-04, TC-INT-03 |
| **Evidence** | `docs/evidence/BUG-011-bill-pay-not-debited.png` |

**Steps to reproduce**

1. Register a new customer and note the balance of the first account (515.50).
2. Open *Bill Pay*, fill in a valid payee, enter `27.75`, choose that account and click **Send Payment**.
3. Check the balance in *Accounts Overview* and through `GET /services/bank/accounts/{id}`, for up to 15 seconds.

**Expected result**

The balance is 487.75 (515.50 − 27.75) in both places.

**Actual result**

The page shows *"Bill Payment Complete"* for the right account, but the balance is still **515.50** in the UI and in the API 15 seconds later. In other runs the same steps debit the account correctly.

**Notes**

- The same payment made directly through the API (`POST /billpay`) was always debited (`BillPayApiTest.billPayDebitsExactAmount`).
- Transfers in the UI showed the same pattern twice on 2026-11-25 (`TransferAcrossLayersTest`): the destination received the money but the source balance did not change, so money was created. The test passed in the next runs, so it stays in the default run while the pattern is investigated.
- The root cause is not known. It may be in how the public demo handles requests from the browser, so it may not reproduce on a local `parasoft/parabank` instance.

**Impact**

- The customer and the payee see a payment as done while the money never left the account, so the books no longer balance.
- Because it is intermittent, it is hard to detect without checking every confirmation against the back end, which is what this test does.

---

## BUG-012 · A loan down payment can overdraw the source account

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Critical: a loan is granted with a down payment the source account does not have |
| **Priority** | High |
| **Area** | Loans · REST API (`POST /services/bank/requestLoan`) |
| **Environment** | https://parabank.parasoft.com/parabank/ · API · found 2026-11-26 |
| **Automated test** | `LoanApiTest.downPaymentAboveSourceBalanceIsRejected` (tag `known-defect`) |
| **Related test cases** | TC-API-LOAN-06 |
| **Related defects** | [BUG-005](#bug-005--transfers-do-not-check-the-available-balance-and-leave-the-account-overdrawn), [BUG-009](#bug-009--bill-payments-do-not-check-the-available-balance-and-leave-the-account-overdrawn) (same missing balance check) |

**Steps to reproduce**

1. Register a new customer (first account: $515.50) and open a second account funded with $100 from the first one.
2. Send `POST /services/bank/requestLoan?customerId={id}&amount=1000&downPayment=300&fromAccountId={second account}`.

**Expected result**

The loan is denied, or the request is rejected, because the source account only has $100 for a $300 down payment.

**Actual result**

The API answers `200 {"loanProviderName":"Wealth Securities Dynamic Loans (WSDL)","approved":true,"accountId":31104}`. A LOAN account is created and the second account goes from **100.00 to -200.00**.

**Notes**

- ParaBank only compares the down payment with the customer's total funds across all non-loan accounts ($515.50 here), not with the balance of the account the down payment comes from.

**Impact**

- The down payment is not really paid: it is taken from money the account does not have.
- Combined with the missing balance checks in transfers and bill payments, any account can be pushed into an overdraft.

---

## BUG-013 · A loan with a negative down payment is approved and adds money to the source account

| Field | Value |
|---|---|
| **Status** | Open |
| **Severity** | Critical: a customer gets a loan and extra money in their account instead of paying a down payment |
| **Priority** | Highest |
| **Area** | Loans · REST API (`POST /services/bank/requestLoan`) |
| **Environment** | https://parabank.parasoft.com/parabank/ · API · found 2026-11-26 |
| **Automated test** | `LoanApiTest.negativeDownPaymentIsRejected` (tag `known-defect`) |
| **Related test cases** | TC-API-LOAN-05 |
| **Related defects** | [BUG-004](#bug-004--transfers-accept-a-negative-amount-which-moves-money-out-of-the-destination-account), [BUG-008](#bug-008--bill-payments-accept-a-negative-amount-which-adds-money-to-the-account) (same missing validation) |

**Steps to reproduce**

1. Log in and note the balance of account A (for example, 465.50).
2. Send `POST /services/bank/requestLoan?customerId={id}&amount=500&downPayment=-200&fromAccountId={A}`.

**Expected result**

The request is rejected because the down payment is negative, no account is created and the balance stays the same.

**Actual result**

The API answers `200 {"loanProviderName":"Wealth Securities Dynamic Loans (WSDL)","approved":true,"accountId":34656}`. A LOAN account is created and account A goes **up** from 465.50 to 665.50.

**Notes**

- The down payment is taken with a withdrawal, and a withdrawal of a negative amount adds money.
- The defect only shows when the loan itself is within the funds rule: a larger loan is denied for insufficient funds before the down payment matters.

**Impact**

- Customers receive cash on top of the loan instead of paying a down payment.
