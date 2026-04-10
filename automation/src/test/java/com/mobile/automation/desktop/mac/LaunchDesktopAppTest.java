package com.mobile.automation.desktop.mac;

import com.mobile.automation.desktop.mac.data.MacDesktopLoginRow;
import com.mobile.automation.desktop.mac.flows.MacDesktopFlow;
import com.mobile.automation.providers.JsonDataProvider;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Sample macOS desktop test using the standard layout: {@link #requireMacDesktopConfig()}, then
 * {@link MacDesktopFlow} for the journey, screenshots at milestones, and room for assertions.
 * Rows come from {@link #macDesktopLoginRows()} (classpath JSON), same style as API suites.
 */
public class LaunchDesktopAppTest extends BaseMacDesktopTest {

    private static final String MAC_LOGIN_DATA = "testdata/desktop/mac-login.json";

    @DataProvider(name = "macDesktopLoginRows")
    public static Object[][] macDesktopLoginRows() {
        return JsonDataProvider.fromClasspath(MAC_LOGIN_DATA, MacDesktopLoginRow.class);
    }

    @Test(
            groups = "mac",
            dataProvider = "macDesktopLoginRows",
            description = "Launch macOS app, verify main window, interact with a UI element")
    public void launchDesktopAppAndInteract(MacDesktopLoginRow data) {
        requireMacDesktopConfig();
        AppiumDriver driver = getDriver();
        attachMacBundleIdentifier(driver);
        Allure.parameter("scenario", data.id());

        MacDesktopFlow app = new MacDesktopFlow(driver, this::attachScreenshot);

        // Act
        app.prepareLoginScreen();
        attachScreenshot("Mac app launched — " + data.id());

        app.loginWithEmailPassword(data.email(), data.password());
        app.submitLogin();
        attachScreenshot("Mac after login submit — " + data.id());

        app.completePinAndReachHome(data.pin());
        attachScreenshot("Mac home dashboard — " + data.id());

        app.logoutFromProfile();
        attachScreenshot("Ajaib Desktop Login screen visible after logout" + data.id());
        app.closeAppWindow();

        // Assert — add explicit checks when you have stable UI markers (e.g. home labels via page API).
    }
}
