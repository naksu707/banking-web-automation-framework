package parabank.utils;

import com.microsoft.playwright.Page;

/**
 * Holds the page of the running test so that listeners (e.g. EvidenceExtension) can reach it.
 * ThreadLocal keeps it safe if tests run in parallel.
 */
public final class PageProvider {

    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();

    private PageProvider() {
    }

    public static void set(Page page) {
        PAGE.set(page);
    }

    public static Page get() {
        return PAGE.get();
    }

    public static void clear() {
        PAGE.remove();
    }
}
