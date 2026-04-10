package com.mobile.automation.desktop.mac.flows;

import com.mobile.automation.desktop.mac.pages.MainWindowPage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Desktop (Mac2) business flows: composes {@link MainWindowPage} only. Use from tests with
 * {@code new MacDesktopFlow(driver, test::attachScreenshot)} for retry debug shots, or
 * {@code new MacDesktopFlow(driver)} without step screenshots.
 */
public class MacDesktopFlow {

    private static final Duration HOME_DASHBOARD_WAIT = Duration.ofSeconds(10);
    private static final Duration VERSI_BARU_PROBE = Duration.ofSeconds(12);
    private static final Duration VERSI_BARU_UPDATE_MAX = Duration.ofSeconds(45);
    private static final Duration LOGIN_TO_PIN_MAX_WAIT = Duration.ofSeconds(90);
    private static final Duration POST_LOGIN_HOME_WAIT = Duration.ofSeconds(60);

    private final Consumer<String> stepScreenshots;
    private final MainWindowPage main;

    public MacDesktopFlow(AppiumDriver driver) {
        this(driver, null);
    }

    public MacDesktopFlow(AppiumDriver driver, Consumer<String> stepScreenshots) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver must not be null");
        }
        this.stepScreenshots = stepScreenshots;
        this.main = new MainWindowPage(driver);
    }

    /** Wait for first interactive UI, maximize, and dismiss optional in-app update if shown. */
    public void prepareLoginScreen() {
        main.waitForMainWindowLoaded();
        main.maximizeWindowBeforeCredentials();
        main.handleOptionalVersiBaruAfterLaunch(VERSI_BARU_PROBE, VERSI_BARU_UPDATE_MAX);
    }

    /**
     * Fills email/password with the same resilience as the sample test (Versi Baru retry, fallback tap).
     */
    public void loginWithEmailPassword(String email, String password) {
        try {
            main.enterCredentials(email, password);
        } catch (TimeoutException | NoSuchElementException | InvalidElementStateException | IllegalStateException e) {
            shot("Mac enterCredentials failed — retry Versi Baru / unblock overlays");
            main.handleOptionalVersiBaruAfterLaunch(VERSI_BARU_PROBE, VERSI_BARU_UPDATE_MAX);
            try {
                main.enterCredentials(email, password);
            } catch (TimeoutException | NoSuchElementException | InvalidElementStateException | IllegalStateException e2) {
                shot("Mac before fallback click to reach login fields");
                main.interactWithFirstAvailableElement();
                shot("Mac main window after fallback overlay/start click");
                main.enterCredentials(email, password);
            }
        }
    }

    public void submitLogin() {
        main.clickSubmitUsingButtons();
    }

    public void completePinAndReachHome(String pin) {
        main.ensurePinStepAfterLoginSubmit(LOGIN_TO_PIN_MAX_WAIT);
        if (main.isLoggedInHomeVisible()) {
            main.waitForHomeDashboardVisible(POST_LOGIN_HOME_WAIT);
            return;
        }
        if (!main.hasPinInputsVisible()) {
            // Some builds transition through a sparse tree before exposing PIN/home markers.
            main.ensurePinStepAfterLoginSubmit(Duration.ofSeconds(20));
            if (main.isLoggedInHomeVisible()) {
                main.waitForHomeDashboardVisible(POST_LOGIN_HOME_WAIT);
                return;
            }
        }
        main.enterPinByInputs(pin);
        main.completePinStep();
        main.waitForHomeDashboardVisible(HOME_DASHBOARD_WAIT);
    }

    public void logoutFromProfile() {
        main.clickProfileAndLogout();
    }

    public void closeAppWindow() {
        main.closeWindowAfterLogin();
    }

    public MainWindowPage mainWindow() {
        return main;
    }

    private void shot(String name) {
        if (stepScreenshots != null) {
            stepScreenshots.accept(name);
        }
    }
}
