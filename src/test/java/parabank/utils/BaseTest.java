package parabank.utils;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import parabank.listeners.EvidenceExtension;
import parabank.listeners.GlobalEvidenceCleaner;

/**
 * Base class for UI tests.
 * Playwright creates a fresh, isolated browser context and page for every test (@UsePlaywright).
 * EvidenceExtension saves a screenshot when a test fails; Playwright keeps the trace.
 */
@Tag("ui")
@UsePlaywright(CustomOptions.class)
@ExtendWith({GlobalEvidenceCleaner.class, EvidenceExtension.class})
public abstract class BaseTest {

    protected Page page;

    @BeforeEach
    void setUpPage(Page page, TestInfo testInfo) {
        this.page = page;
        page.setDefaultTimeout(Config.timeout());
        PlaywrightAssertions.setDefaultAssertionTimeout(Config.timeout());
        PageProvider.set(page);
        Logs.info("▶ {}", testInfo.getDisplayName());
    }

    @AfterEach
    void tearDownPage() {
        PageProvider.clear();
    }
}
