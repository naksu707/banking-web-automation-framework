package parabank.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

/** Elements shared by every ParaBank page. */
public abstract class BasePage {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    public Page page() {
        return page;
    }

    /** Main heading of the right panel, e.g. "Accounts Overview". */
    public Locator title() {
        return page.locator("#rightPanel h1.title").first();
    }

    /** "Welcome <first name> <last name>" text in the left panel, shown after login. */
    public Locator welcomeMessage() {
        return page.locator("#leftPanel p.smallText");
    }

    @Step("Log out")
    public HomePage logOut() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Log Out")).click();
        return new HomePage(page);
    }
}
