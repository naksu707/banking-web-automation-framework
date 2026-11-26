# Test Plan · Banking Web Automation Framework

| | |
|---|---|
| **Application under test** | [ParaBank](https://parabank.parasoft.com/parabank/), a demo online bank by Parasoft |
| **Owner** | María José Ospina Samboni |
| **Last updated** | 2026-11-26 |
| **Related documents** | [Test strategy](test-strategy.md) · [Test cases](test-cases.md) · [Defects](defects.md) |

---

## 1. Scope

### In scope

| Module | UI | API | UI vs. API | Test cases |
|---|:---:|:---:|:---:|---|
| Authentication (login, logout) | ✅ | ✅ | | TC-AUTH, TC-API-AUTH |
| Registration | ✅ | | | TC-REG |
| Accounts overview and open new account | ✅ | ✅ | ✅ | TC-ACC, TC-API-ACC, TC-INT-01, TC-INT-02 |
| Transfer funds | ✅ | ✅ | ✅ | TC-TRF, TC-API-TRF, TC-INT-03 |
| Bill pay | ✅ | ✅ | ✅ | TC-BILL, TC-API-BILL, TC-INT-04 |
| Request loan | ✅ | ✅ | ✅ | TC-LOAN, TC-API-LOAN, TC-INT-05 |
| Transaction history (Find Transactions) | ✅ | ✅ | ✅ | TC-TXN, TC-API-TXN, TC-INT-06 |

The full list, with priorities and the automated test for each case, is in **[test-cases.md](test-cases.md)**.

### Out of scope

- Update contact info, the admin page and the web services other than the REST API.
- Performance, load, security scanning and accessibility audits.
- Mobile browsers and screen sizes.

## 2. Approach

Testing follows the [test strategy](test-strategy.md). In short:

- Every module has **positive**, **negative** and **business-rule** cases, on the UI and on the API.
- Every money operation is checked for **exact amounts** and **money conservation** across all accounts involved.
- An **integration test** per module performs the action in the UI and checks the result in the back end through the API.
- Input values for validation cases live in **CSV files** (`src/test/resources/data`).

## 3. Test environment

| Item | Value |
|---|---|
| CI | GitHub Actions, `ubuntu-latest`, ParaBank in Docker (`parasoft/parabank:latest`) |
| Browsers | Chromium, Firefox and WebKit (one CI job each) |
| Java | 17 (Temurin) |
| Local runs | Public demo by default; Docker with `-DbaseUrl=http://localhost:8080/parabank/` |

## 4. Schedule and status

| Phase | Content | Status |
|---|---|---|
| Phase 1 | Framework core, authentication, registration, accounts overview, CI and Allure | ✅ Done |
| Phase 2 | Open account, transfers, bill pay, loans and transaction history | ✅ Done |
| Phase 3 | Data-driven tests, cross-browser CI, strategy and plan documents | ✅ Done |

## 5. Deliverables

- Automated test suite (this repository).
- Allure report for every CI run, per browser (artifact `allure-report-<browser>`).
- [Test cases](test-cases.md) with traceability to the automated tests.
- [Defect reports](defects.md) with evidence in `docs/evidence`.
- This plan and the [test strategy](test-strategy.md).

## 6. Entry and exit criteria

**Entry**
- ParaBank answers at the configured base URL.
- A new customer can be registered.

**Exit**
- All tests outside `known-defect` pass in CI on every browser of the matrix.
- Every open defect is documented and has a `known-defect` test that reproduces it.

## 7. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| The public demo rate limits the IP (HTTP 429) | Local runs fail without a real defect | CI uses a private Docker instance; locally, run only the class being worked on; clear 429 messages |
| The public demo is shared and can be reset | Data used by a test changes or disappears | Each test class registers its own customer and reads balances at the start |
| The UI takes a moment to show changes made in the back end | False failures in integration tests | Balance checks poll for up to 15 seconds before failing |
| Intermittent defects (BUG-011) | Random pipeline failures | The test is tagged `known-defect` and the defect is documented as intermittent |
| Defects found on the public demo may not exist in Docker | `known-defect` tests may pass in CI | Run the `known-defect` group against both environments before closing a defect |

## 8. Results summary

At the end of Phase 2 the suite has **66 automated test methods** (30 UI, 30 API, 6 integration), covering **74 documented test cases**. Testing found **14 defects, 9 of them Critical**, all documented in [defects.md](defects.md):

- **Access control:** wrong credentials open another customer's account (BUG-003); Find Transactions shows other customers' transactions (BUG-014).
- **Money integrity:** negative amounts accepted in transfers, bill payments and loans (BUG-004, BUG-008, BUG-013); overdrafts allowed (BUG-005, BUG-009, BUG-012); payments confirmed without a debit (BUG-011).
