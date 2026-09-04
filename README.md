# Banking Web Automation Framework

Automated testing framework for **ParaBank**, a demo online banking application provided by Parasoft for software testing and automation practice.

This project focuses on **Web UI and API test automation**, applying professional QA practices to validate simulated banking workflows such as authentication, account management, fund transfers, bill payments, transactions, and loan requests.

> **Important:** ParaBank is a simulated banking application created for testing and demonstration purposes. This project does not interact with real banking systems, financial accounts, or real transactions.

---

## Project Overview

The purpose of this project is to design and implement a maintainable **Banking Test Automation Framework** using an existing banking simulator as the Application Under Test (AUT).

The framework will automate both:

* Web UI workflows using Selenium WebDriver.
* REST API workflows using REST Assured.

The project demonstrates how QA engineers can combine UI and API automation to validate the same business processes from different layers of an application.

---

## Objectives

* Build a maintainable automation framework.
* Automate critical banking workflows.
* Implement UI automation with Selenium WebDriver.
* Implement API automation with REST Assured.
* Apply the Page Object Model.
* Create reusable automation components.
* Validate positive and negative scenarios.
* Validate API responses and business rules.
* Implement regression testing.
* Generate automated test reports.
* Integrate automated tests into CI/CD.

---

## Application Under Test

### ParaBank

**Type:** Simulated Online Banking Application

ParaBank provides a simulated banking environment where users can perform operations such as:

* Registering users.
* Logging in.
* Managing accounts.
* Viewing balances.
* Transferring funds.
* Paying bills.
* Viewing transactions.
* Requesting loans.

The application also exposes REST services that can be used for API automation.

---

## Testing Scope

### Authentication

#### Positive Scenarios

* Login with valid credentials.
* Successful logout.
* Successful user registration.

#### Negative Scenarios

* Invalid username.
* Invalid password.
* Empty username.
* Empty password.
* Invalid registration information.

---

### Account Management

* View account overview.
* Validate account information.
* Validate account balance.
* Open a new account.
* Validate successful account creation.
* Retrieve account information through the API.

---

### Fund Transfers

* Transfer funds between accounts.
* Validate transfer amount.
* Validate source account.
* Validate destination account.
* Validate successful transfer.
* Validate invalid transfer scenarios.
* Validate transfer through the API.

Example automated flow:

```text
Login
  ↓
Account Overview
  ↓
Select Source Account
  ↓
Select Destination Account
  ↓
Enter Transfer Amount
  ↓
Submit Transfer
  ↓
Validate Transfer Result
```

---

### Bill Payment

* Access bill payment functionality.
* Enter payee information.
* Enter payment information.
* Submit payment.
* Validate successful payment.
* Validate required fields.
* Validate invalid payment data.

---

### Transactions

* View transaction history.
* Validate transaction details.
* Search transactions.
* Validate transaction amounts.
* Validate transaction dates.
* Retrieve transactions through the API.

---

### Loan Requests

* Access loan request functionality.
* Enter loan information.
* Submit loan request.
* Validate loan response.
* Validate approved scenarios.
* Validate rejected scenarios.

---

## API Automation

The project will use **REST Assured** to validate ParaBank's available REST services.

API testing will cover operations related to:

* Authentication.
* Customers.
* Accounts.
* Transactions.
* Transfers.
* Bill payments.
* Loans.

### API Validations

Each API test may validate:

* HTTP status code.
* Response body.
* Response structure.
* Required fields.
* Data types.
* Business rules.
* Error responses.
* Response time.
* Data consistency.

Example:

```text
Test
 ↓
REST Assured
 ↓
HTTP Request
 ↓
ParaBank REST API
 ↓
HTTP Response
 ↓
Assertions
 ↓
Test Result
```

---

## UI Automation

The web application will be automated using **Selenium WebDriver**.

UI tests will validate:

* Page navigation.
* Form interactions.
* Element visibility.
* User input.
* Business workflows.
* Error messages.
* Dynamic content.
* User-facing results.

The UI automation layer will use the **Page Object Model** to separate page interactions from test logic.

---

## QA Strategy

The project follows a risk-based testing approach.

### Critical

* Login.
* Account access.
* Fund transfers.
* Bill payments.
* Transaction validation.

### High

* User registration.
* Account creation.
* Loan requests.
* Customer information.

### Medium

* Transaction searches.
* Additional form validations.
* Secondary navigation scenarios.

---

## Test Types

The framework will include:

### Functional Testing

Validates that application functionality behaves according to expected requirements.

### Negative Testing

Validates system behavior when invalid data or actions are introduced.

### Regression Testing

Ensures existing functionality remains stable after changes.

### Smoke Testing

Validates the application's most critical workflows.

### UI Testing

Validates the web interface through Selenium WebDriver.

### API Testing

Validates backend services through REST Assured.

### Integration Testing

Validates consistency between different application layers where appropriate.

---

## Framework Architecture

```text
                 ┌─────────────────────┐
                 │      JUnit Tests    │
                 └──────────┬──────────┘
                            │
              ┌─────────────┴─────────────┐
              │                           │
              ▼                           ▼
     ┌────────────────┐         ┌────────────────┐
     │   UI Tests     │         │   API Tests    │
     │    Selenium    │         │ REST Assured   │
     └───────┬────────┘         └───────┬────────┘
             │                          │
             ▼                          ▼
     ┌────────────────┐         ┌────────────────┐
     │  Page Objects  │         │   API Clients  │
     └───────┬────────┘         └───────┬────────┘
             │                          │
             └────────────┬─────────────┘
                          ▼
                 ┌─────────────────┐
                 │ Test Utilities  │
                 │ Config / Data   │
                 │ Assertions      │
                 │ Logging         │
                 └────────┬────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │    ParaBank     │
                 │   Simulated AUT │
                 └─────────────────┘
```

---

## Technology Stack

| Technology         | Purpose                         |
| ------------------ | ------------------------------- |
| Java               | Programming language            |
| Selenium WebDriver | Web UI automation               |
| REST Assured       | REST API automation             |
| JUnit              | Test execution                  |
| Maven              | Build and dependency management |
| Page Object Model  | UI automation architecture      |
| Git                | Version control                 |
| GitHub             | Source code management          |
| GitHub Actions     | CI/CD                           |
| Allure             | Test reporting                  |

---

## End-to-End Automation Flow

A representative business workflow:

```text
Open ParaBank
      ↓
Register / Login
      ↓
Access Account
      ↓
Validate Balance
      ↓
Transfer Funds
      ↓
Validate Transaction
      ↓
Pay Bill
      ↓
Validate Payment
      ↓
Review Transaction History
      ↓
Logout
```

The same business operations can also be validated independently through the REST API.

---

## Automation Design Principles

### Page Object Model

Each important page will have its own class.

```text
LoginPage
HomePage
AccountsOverviewPage
OpenAccountPage
TransferFundsPage
BillPayPage
FindTransactionsPage
UpdateContactPage
RequestLoanPage
```

### API Client Layer

API operations will be separated from the test classes.

```text
AuthApi
CustomerApi
AccountsApi
TransactionsApi
TransferApi
BillPayApi
LoanApi
```

### Reusable Components

Common functionality will be centralized to reduce code duplication.

Examples:

* WebDriver management.
* Browser configuration.
* API configuration.
* Request specifications.
* Response validation.
* Test data management.
* Screenshots.
* Logging.
* Custom assertions.

---

## Test Reporting

The framework will generate automated reports containing:

* Test execution status.
* Execution duration.
* Passed tests.
* Failed tests.
* Error details.
* Screenshots.
* API information.
* Execution history.

**Allure Reports** will be used for visual test reporting.

---

## CI/CD

The automation framework will be integrated with **GitHub Actions**.

The pipeline will execute the automated test suite after configured repository events.

```text
Git Push / Pull Request
          ↓
    GitHub Actions
          ↓
    Build Project
          ↓
   Execute UI Tests
          ↓
   Execute API Tests
          ↓
    Generate Report
          ↓
    Publish Results
```

---

## Execution

### Prerequisites

Install:

* Java
* Maven
* Git

### Clone Repository

```bash
git clone <repository-url>
cd banking-web-automation-framework
```

### Install Dependencies

```bash
mvn clean install
```

### Run All Tests

```bash
mvn test
```

### Run UI Tests

```bash
mvn test -Dtest="ui.*"
```

### Run API Tests

```bash
mvn test -Dtest="api.*"
```

---

## Test Documentation

The project will include:

* Test Strategy.
* Test Plan.
* Test Cases.
* API Test Cases.
* Test Data.
* Requirements Traceability Matrix.
* Automation Architecture.
* Regression Suite.
* Defect Documentation.
* Test Evidence.
* CI/CD Documentation.

---

## Future Improvements

* Parallel test execution.
* Cross-browser testing.
* Data-driven testing.
* API schema validation.
* Contract testing.
* Dockerized execution.
* Advanced Allure reporting.
* Automatic screenshots on failures.
* Video recording on failures.
* Environment-based configuration.
* Expanded CI/CD pipeline.
* Integration with test management tools.

---

## Portfolio Goals

This project demonstrates practical experience in:

* Selenium WebDriver.
* Java.
* JUnit.
* REST Assured.
* UI automation.
* API automation.
* End-to-end testing.
* Functional testing.
* Negative testing.
* Regression testing.
* Page Object Model.
* Test architecture.
* CI/CD.
* Automated reporting.
* QA engineering practices.

The project demonstrates the ability to build a **maintainable automation framework that validates both frontend behavior and backend services using a simulated banking environment**.

---

## Disclaimer

This project uses **ParaBank solely as a test and demonstration environment**.

No real bank accounts, real financial transactions, payment information, or real customer data are involved.

The purpose of the project is exclusively **software testing, test automation, and QA portfolio development**.

---

## Project Status

**Status:** In Development

The framework is being developed incrementally, beginning with authentication and core banking workflows and progressively expanding UI automation, API coverage, regression testing, reporting, and CI/CD.

---

## License

This project is intended for educational and portfolio purposes.
