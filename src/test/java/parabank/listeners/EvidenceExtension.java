package parabank.listeners;

import com.microsoft.playwright.Page;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import parabank.utils.FileManager;
import parabank.utils.Logs;
import parabank.utils.PageProvider;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

/**
 * Saves evidence when a UI test fails: a full-page screenshot in target/evidence,
 * also attached to the Allure report.
 * Runs right after the test method, while the page is still open.
 */
public class EvidenceExtension implements AfterTestExecutionCallback {

    @Override
    public void afterTestExecution(ExtensionContext context) {
        String testName = context.getRequiredTestClass().getSimpleName() + "-" + context.getDisplayName();

        if (context.getExecutionException().isEmpty()) {
            Logs.info("✔ PASSED: {}", testName);
            return;
        }

        Logs.error("✘ FAILED: {} → {}", testName, context.getExecutionException().get().getMessage());

        Page page = PageProvider.get();
        if (page == null || page.isClosed()) {
            Logs.warn("No open page to capture for {}", testName);
            return;
        }

        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        Path file = FileManager.saveScreenshot(testName, screenshot);
        Allure.addAttachment("Screenshot on failure", "image/png", new ByteArrayInputStream(screenshot), ".png");
        Logs.info("Screenshot saved: {}", file);
    }
}
