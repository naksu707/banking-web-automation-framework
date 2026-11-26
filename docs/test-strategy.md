# Test Strategy · Banking Web Automation Framework

| | |
|---|---|
| **Application under test** | [ParaBank](https://parabank.parasoft.com/parabank/), a demo online bank by Parasoft |
| **Owner** | María José Ospina Samboni |
| **Last updated** | 2026-11-26 |
| **Related documents** | [Test plan](test-plan.md) · [Test cases](test-cases.md) · [Defects](defects.md) |

This document explains **how** the application is tested and **why** the approach was chosen. The [test plan](test-plan.md) describes **what** is tested in this release.

---

## 1. Objectives

1. Confirm that the core banking workflows work end to end: registration, login, accounts, transfers, bill payment, loans and transaction history.
2. Protect **money integrity**: every operation moves exactly the expected amount, and nothing else changes.
3. Protect **access control**: a customer can only see and use their own data.
4. Confirm that the **UI and the API agree**: what the customer sees in the browser is what the back end stored.
5. Find, document and track defects, with evidence and automated tests that reproduce them.

## 2. Test levels

| Level | What it covers | Why |
|---|---|---|
| **API** (REST, `services/bank`) | Business rules, calculations, status codes, data stored in the back end | Fast and stable; checks the rules without the browser |
| **UI** (Chromium, Firefox, WebKit) | What the customer does and sees: forms, validation messages, confirmations, navigation | Catches problems the API alone cannot, such as validation done only in the browser |
| **Integration** (UI + API) | An action in the UI, checked in the back end through the API | Catches confirmations that do not match the stored data (see BUG-011) |

Unit testing is out of scope: ParaBank is a third-party application, tested here as a black box.

## 3. Risk-based priorities

Effort goes first to the areas where a defect costs the most:

| Priority | Area | Main risks |
|---|---|---|
| **Critical** | Authentication, account access, transfers, bill payments, loans | Access to another customer's account; money created, lost or moved without a valid reason |
| **High** | Registration, transaction history, UI vs. API consistency | Wrong or missing records; another customer's data exposed |
| **Medium** | Validation messages, navigation, error pages | A confusing experience, but no money or data at risk |

Every module is tested with **positive** cases (the expected path), **negative** cases (invalid, empty, negative, zero and excessive amounts) and **business-rule** cases (balances, limits, ownership).

## 4. Test design techniques

- **Equivalence partitioning and boundary values** for amounts: valid, empty, non-numeric, zero, negative and greater than the balance.
- **Business-rule testing** from ParaBank's own source code, for example the loan approval rules (down payment and available funds).
- **Money-conservation checks:** read the balances before and after each operation and check the exact difference, in every account involved.
- **Access-control checks:** use one customer's session with another customer's ids.
- **Data-driven tests:** input values and expected messages live in CSV files under `src/test/resources/data`, so new cases need no code changes.

## 5. Test data

- Every test class **registers its own customer** through the UI (the API has no registration endpoint), so tests never depend on shared demo accounts.
- Amounts with random cents (for example `43.17`) make each test's transactions easy to find.
- Tests read the balances they need at the start, instead of assuming fixed values.

## 6. Environments

| Environment | Use | Notes |
|---|---|---|
| Public demo (`parabank.parasoft.com`) | Local development and exploratory testing | Shared and rate limited by Cloudflare (HTTP 429); the database can be reset at any time |
| Docker (`parasoft/parabank`) | CI on GitHub Actions, and locally with `-DbaseUrl` | Private and predictable; started fresh for every run |

## 7. Tools

| Need | Tool |
|---|---|
| Language and build | Java 17, Maven |
| Browser and API automation | Playwright for Java |
| Test runner | JUnit 5 (tags, parameterized and data-driven tests) |
| Reporting | Allure (steps, attachments, severity) |
| CI | GitHub Actions, with a browser matrix |
| Logging and evidence | Log4j 2, screenshots and Playwright traces on failure |

## 8. Automation approach

- **Page Object Model** for the UI and a dedicated **API client**, so tests read as business steps.
- Configuration from `config.properties`, overridable with environment variables and `-D` flags (browser, headless mode, base URL).
- Tags select what runs: `smoke` for a quick check, `ui`, `api` and `integration` by layer, and `known-defect` for tests that expose open defects.
- **Known defects do not break the pipeline:** tests that expose an open defect are tagged `known-defect` and excluded from the default run. They are run on demand with `mvn test -Dgroups=known-defect -DexcludedGroups=` to check whether a defect was fixed.

## 9. Defect management

- Each defect gets an id, severity, priority, steps to reproduce, expected and actual results, impact and, when useful, a screenshot in `docs/evidence`.
- Severity follows the impact on the customer and the bank: **Critical** for money or data at risk, **Medium** for wrong but harmless behavior, **Low** for cosmetic or meaningless results.
- A defect is only reported after it is reproduced and other causes are ruled out (slow responses, rate limiting, test data). Intermittent defects are labeled as such.

## 10. Entry and exit criteria

**Entry:** the environment answers, test customers can be registered, and the code compiles.

**Exit:**
- All tests outside `known-defect` pass in CI on Chromium, Firefox and WebKit.
- Every failing check is either fixed in the tests or documented as a defect with a `known-defect` test.
- Test cases, defects and the README are updated.

## 11. Known limitations

- The public demo is shared, rate limited and can be reset, so local failures caused by the site are not defects in the application.
- Some defects found on the public demo may not reproduce on a fresh Docker instance.
- Performance, load and accessibility testing are out of scope for this project.
