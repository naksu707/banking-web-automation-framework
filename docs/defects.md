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
