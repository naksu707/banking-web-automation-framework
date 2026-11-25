package parabank.listeners;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import parabank.utils.FileManager;
import parabank.utils.Logs;

import java.util.concurrent.atomic.AtomicBoolean;

/** Deletes the evidence of previous runs once, before the first test class starts. */
public class GlobalEvidenceCleaner implements BeforeAllCallback {

    private static final AtomicBoolean CLEANED = new AtomicBoolean(false);

    @Override
    public void beforeAll(ExtensionContext context) {
        if (CLEANED.compareAndSet(false, true)) {
            FileManager.cleanEvidence();
            Logs.info("Evidence folder cleaned: {}", FileManager.EVIDENCE_DIR);
        }
    }
}
