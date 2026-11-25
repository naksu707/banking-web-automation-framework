<div align="center">

# 🏦 Banking Web Automation Framework
### UI and REST API test automation for a banking application, with Playwright for Java

<p>
<img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" /> <img src="https://img.shields.io/badge/Playwright-2EAD33?style=for-the-badge&logo=playwright&logoColor=white" /> <img src="https://img.shields.io/badge/JUnit%205-25A162?style=for-the-badge&logo=junit5&logoColor=white" /> <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" />
<br>
<img src="https://img.shields.io/badge/Allure-FF6C37?style=for-the-badge" /> <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" />
</p>

</div>

> [!NOTE]
> **Status: in progress 🚧** · Phase 1 (framework core and authentication) is complete. Phase 2 (core banking workflows) has started with opening accounts; transfers and bill payment are next.

---

## 💡 About

A test automation framework for **[ParaBank](https://parabank.parasoft.com/parabank/)**, a demo online bank by Parasoft. It validates the same business flows through the **web UI** and the **REST API**, and checks that both layers agree.

> ParaBank is a public demo application with simulated data. No real accounts or money are involved.

**Design goals**

- **Maintainable:** Page Object Model for the UI and a dedicated client for the API.
- **Independent tests:** every run registers its own customers, so tests never depend on shared demo accounts that other people can change.
- **Easy to debug:** failed UI tests save a screenshot and a Playwright trace, and every run writes a log.
- **One tool for both layers:** Playwright drives the browser and also sends the API requests.

---

## 🧪 Test scope

| Area | UI | API | Status |
|---|---|---|---|
| Login, logout and registration (positive and negative) | ✅ | ✅ | Done |
| Accounts overview (UI vs. API consistency) | ✅ | ✅ | Done |
| Open new account (checking and savings, funding, UI vs. API) | ✅ | ✅ | Done |
| Transfer funds | | | Planned |
| Bill payment | | | Planned |
| Transaction history | | | Planned |
| Loan request | | | Planned |

Detailed test cases and their traceability: **[docs/test-cases.md](docs/test-cases.md)**.

### 🐞 Defects found

| ID | Defect | Severity | Status |
|---|---|---|---|
| [BUG-003](docs/defects.md#bug-003--wrong-credentials-open-another-customers-account) | **Wrong credentials open another customer's account** (authentication bypass in the web login) | **Critical** | Open |
| [BUG-001](docs/defects.md#bug-001--wrong-credentials-show-an-internal-server-error-instead-of-a-login-error) | Wrong credentials show *"An internal error has occurred"* instead of a login error | Medium | Open |
| [BUG-002](docs/defects.md#bug-002--the-createaccount-api-response-reports-a-balance-of-0-for-a-funded-account) | `POST /createAccount` responds with a balance of 0, although the new account holds the initial deposit | Low | Open |

Tests that expose an open defect are tagged `known-defect` and skipped by default, so the pipeline stays green while the defect stays documented.

### Risk-based priorities

| Priority | Features |
|---|---|
| **Critical** | Login, account access, transfers, bill payments, transaction validation |
| **High** | Registration, opening accounts, loans, customer information |

---

## 🛠️ Tech stack

| Purpose | Tool |
|---|---|
| Language | Java 17 |
| UI and API automation | Playwright for Java (`Page`, `Locator`, `APIRequestContext`) |
| Test runner | JUnit 5 (tags, parameterized tests) |
| Build | Maven |
| Reporting | Allure (steps, screenshots, API responses) and Playwright traces |
| Logging | Log4j2 (console and `target/logs/tests.log`) |
| CI/CD | GitHub Actions |

---

## 🏗️ Architecture

```text
src/test
├── java/parabank
│   ├── api/            ParaBankApiClient (login, customers, accounts, createAccount) and models
│   ├── data/           Customer, AccountType, TestDataFactory (unique data), TestUsers (fresh customers)
│   ├── listeners/      EvidenceExtension (screenshot on failure), GlobalEvidenceCleaner
│   ├── pages/          Page Objects: BasePage, HomePage, RegisterPage, AccountsOverviewPage,
│   │                   OpenAccountPage, AccountActivityPage
│   ├── tests/
│   │   ├── ui/         LoginTest, LogoutTest, RegistrationTest, OpenAccountTest
│   │   ├── api/        AuthApiTest, AccountsApiTest
│   │   └── integration/ AccountsAcrossLayersTest, OpenAccountAcrossLayersTest (UI vs. API)
│   └── utils/          BaseTest, BaseApiTest, Config, CustomOptions, FileManager, Logs, PageProvider
└── resources/
    ├── config.properties
    └── log4j2.xml
```

**How a test runs:** `@UsePlaywright(CustomOptions.class)` gives every test a fresh, isolated browser context and page (or API context), so cookies and sessions never leak between tests. `PageProvider` shares the page with the listeners, and `EvidenceExtension` captures a screenshot when a test fails.

---

## ▶️ Run the tests

**Requirements:** Java 17+ and Maven 3.9+. Playwright downloads the browsers automatically on the first run.

```bash
# All tests
mvn test

# Only one group (tags: smoke, ui, api, integration)
mvn test -Dgroups=smoke

# Tests that expose known application defects (skipped by default)
mvn test -Dgroups=known-defect -DexcludedGroups=

# Watch the browser, slowed down
mvn test -Dheadless=false -DslowMo=500

# Another browser: chromium (default), firefox or webkit
mvn test -Dbrowser=firefox

# One test class
mvn test -Dtest=LoginTest
```

### Configuration

Defaults are in `src/test/resources/config.properties`. Any value can be overridden with a `-D` flag or an environment variable:

| Key | Default | Env variable |
|---|---|---|
| `baseUrl` | `https://parabank.parasoft.com/parabank/` | `BASE_URL` |
| `browser` | `chromium` | `BROWSER` |
| `headless` | `true` | `HEADLESS` |
| `slowMo` | `0` | `SLOW_MO` |
| `timeout` | `15000` (ms) | `TIMEOUT` |

---

## 📊 Reports

```bash
mvn allure:serve
```

Each test shows its steps and API calls attach their JSON responses. When a UI test fails:

- a **full-page screenshot** is attached to the report and saved in `target/evidence`;
- a **Playwright trace** is saved in `target/playwright`: open it at [trace.playwright.dev](https://trace.playwright.dev) to replay every action, network call and DOM snapshot;
- the run log is in `target/logs/tests.log`.

---

## ⚙️ CI/CD

GitHub Actions runs the suite on every push and pull request to `main`, and can be started manually choosing which tags to run. The Allure report and the traces of failed tests are uploaded as artifacts.

---

## 🗺️ Roadmap

- [x] Framework core: configuration, Page Objects, API client, test data, failure artifacts
- [x] Authentication: login, logout and registration (UI and API)
- [x] UI vs. API consistency for the accounts overview
- [x] CI with GitHub Actions and Allure report
- [x] Open new account (UI, API and UI vs. API)
- [ ] Transfer funds and bill payment
- [ ] Transaction history and loan requests
- [ ] Data-driven tests from external files
- [ ] Cross-browser matrix in CI
- [ ] Test strategy and test plan documents

---

## ⚠️ Known limitations

- ParaBank is a shared public demo: it can be slow, return temporary errors, or reset its database. A failure caused by the site being unavailable is not a defect in the tests.
- ParaBank's API has no registration endpoint, so test customers are always registered through the UI.
- **CI uses its own ParaBank:** GitHub Actions starts the official `parasoft/parabank` Docker image and runs the suite against `http://localhost:8080/parabank/`, so the pipeline does not depend on the public demo being available. Locally the tests use the public site by default; to use a local copy run `docker run -d -p 8080:8080 parasoft/parabank` and `mvn test -DbaseUrl=http://localhost:8080/parabank/`. Defects found on the public site (BUG-001, BUG-003) may not reproduce on a fresh local instance.
- **Rate limiting:** ParaBank sits behind Cloudflare, which blocks an IP that sends too many requests in a short time (HTTP 429, *"Error 1015: You are being rate limited"*). While blocked, pages do not load and UI tests time out waiting for elements. Running the full suite many times in a row triggers it. Wait 15–30 minutes, and during development run only the class you are working on (`mvn test -Dtest=LoginTest`). The API client reports a 429 with a clear message, and test customer registration retries up to 3 times before failing with *"ParaBank may be unavailable"*.

---

<div align="center">

Made with 💜 by **[María José Ospina Samboni](https://github.com/naksu707)**

</div>
