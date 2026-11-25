package parabank.utils;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.junit.UsePlaywright;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import parabank.api.ParaBankApiClient;

/** Base class for REST API tests. Playwright injects a fresh APIRequestContext for every test. */
@Tag("api")
@UsePlaywright(CustomOptions.class)
public abstract class BaseApiTest {

    protected ParaBankApiClient api;

    @BeforeEach
    void setUpApi(APIRequestContext request, TestInfo testInfo) {
        this.api = new ParaBankApiClient(request);
        Logs.info("▶ {}", testInfo.getDisplayName());
    }
}
