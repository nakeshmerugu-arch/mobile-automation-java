package com.mobile.automation.desktop.mac.flows;

import com.mobile.automation.desktop.mac.pages.MainWindowPage;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
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

    private static final Duration VERSI_BARU_PROBE = Duration.ofSeconds(3);
    private static final Duration VERSI_BARU_UPDATE_MAX = Duration.ofSeconds(12);
    /** After login submit: wait for PIN or home. Override with {@code -Dmac.loginToPinWaitSeconds=90}. */
    private static final Duration LOGIN_TO_PIN_MAX_WAIT = Duration.ofSeconds(parseLoginToPinWaitSeconds());
    private static final Duration POST_PIN_HOME_WAIT = Duration.ofSeconds(6);
    private static final String TIMING_FLAG = "mac.timing.enabled";

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
        runTimedStep("prepareLoginScreen", () -> {
            main.waitForMainWindowLoaded();
            main.maximizeWindowBeforeCredentials();
            main.handleOptionalVersiBaruAfterLaunch(VERSI_BARU_PROBE, VERSI_BARU_UPDATE_MAX);
        });
    }

    /**
     * Fills email/password with the same resilience as the sample test (Versi Baru retry, fallback tap).
     */
    public void loginWithEmailPassword(String email, String password) {
        runTimedStep("loginWithEmailPassword", () -> {
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
        });
    }

    public void submitLogin() {
        runTimedStep("submitLogin", main::clickSubmitUsingButtons);
    }

    public void completePinAndReachHome(String pin) {
        runTimedStep("completePinAndReachHome", () -> {
            main.ensurePinStepAfterLoginSubmit(LOGIN_TO_PIN_MAX_WAIT);
            if (main.isLoggedInHomeVisible()) {
                main.waitForHomeDashboardVisible(POST_PIN_HOME_WAIT);
                return;
            }
            if (!main.hasPinInputsVisible()) {
                // Some builds transition through a sparse tree before exposing PIN/home markers.
                main.ensurePinStepAfterLoginSubmit(Duration.ofSeconds(8));
                if (main.isLoggedInHomeVisible()) {
                    main.waitForHomeDashboardVisible(POST_PIN_HOME_WAIT);
                    return;
                }
            }
            main.enterPinByInputs(pin);
            main.completePinStep();
            // If PIN UI disappeared, consider the flow resolved and let next steps validate continuity.
            if (!main.hasPinInputsVisible()) {
                return;
            }
            main.waitForHomeDashboardVisible(POST_PIN_HOME_WAIT);
        });
    }

    public void logoutFromProfile() {
        runTimedStep("openProfileMenu", main::openProfileMenu);
        runTimedStep("clickLogoutAction", main::clickLogoutAction);
        runTimedStep("waitForLoginScreenVisible", () -> main.waitForLoginScreenVisible(Duration.ofSeconds(45)));
    }

    public void closeAppWindow() {
        runTimedStep("closeAppWindow", main::closeWindowAfterLogin);
    }

    public MainWindowPage mainWindow() {
        return main;
    }

    private void shot(String name) {
        if (stepScreenshots != null) {
            stepScreenshots.accept(name);
        }
    }

    private static int parseLoginToPinWaitSeconds() {
        try {
            int s = Integer.parseInt(System.getProperty("mac.loginToPinWaitSeconds", "90"));
            return Math.max(15, Math.min(s, 300));
        } catch (NumberFormatException e) {
            return 90;
        }
    }

    private static boolean isTimingEnabled() {
        return Boolean.parseBoolean(System.getProperty(TIMING_FLAG, "false"));
    }

    private static void runTimedStep(String step, Runnable action) {
        if (!isTimingEnabled()) {
            action.run();
            return;
        }
        long started = System.nanoTime();
        try {
            action.run();
        } finally {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            String msg = "[mac-timing] " + step + " took " + elapsedMs + " ms";
            System.out.println(msg);
            try {
                Allure.addAttachment("mac timing: " + step, "text/plain", msg);
            } catch (Exception ignored) {
                // Allure attachment is best-effort only
            }
        }
    }
}
