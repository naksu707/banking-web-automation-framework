package parabank.utils;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

import java.nio.file.Paths;
import java.util.Map;

/**
 * Playwright settings used by @UsePlaywright.
 * Browser, headless mode, speed and URL come from Config, so they can be changed with -D flags.
 * Traces are kept only for failed tests, in target/playwright.
 */
public class CustomOptions implements OptionsFactory {

    @Override
    public Options getOptions() {
        return new Options()
                .setBrowserName(Config.browser())
                .setHeadless(Config.headless())
                .setBaseUrl(Config.baseUrl())
                .setLaunchOptions(new BrowserType.LaunchOptions()
                        .setSlowMo(Config.slowMo()))
                .setContextOptions(new Browser.NewContextOptions()
                        .setBaseURL(Config.baseUrl())
                        .setViewportSize(1366, 900))
                .setApiRequestOptions(new APIRequest.NewContextOptions()
                        .setBaseURL(Config.baseUrl())
                        .setTimeout(Config.timeout())
                        .setExtraHTTPHeaders(Map.of("Accept", "application/json")))
                .setTrace(Options.Trace.RETAIN_ON_FAILURE)
                .setOutputDir(Paths.get("target", "playwright"));
    }
}
