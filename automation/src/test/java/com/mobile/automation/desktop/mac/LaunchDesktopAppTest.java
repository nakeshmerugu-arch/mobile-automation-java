package com.mobile.automation.desktop.mac;

import com.mobile.automation.desktop.mac.pages.MainWindowPage;
import com.mobile.automation.config.DriverConfig;
import com.mobile.automation.config.DriverConfigLoader;
import com.mobile.automation.driver.DriverManager;
import com.mobile.automation.tests.BaseTest;

import io.appium.java_client.AppiumDriver;

import org.openqa.selenium.Capabilities;
import io.qameta.allure.Allure;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Sample macOS desktop test.
 * Requires platform/platformName configured as "mac" so DriverFactory creates a Mac2Driver.
 */
public class LaunchDesktopAppTest extends BaseTest {

    private static final Duration HOME_DASHBOARD_WAIT = Duration.ofSeconds(10);
    @SuppressWarnings("unused")
    private static final Duration LOGIN_AFTER_LOGOUT_WAIT = Duration.ofSeconds(10);
    /** How long to watch for "Versi Baru Tersedia" after maximize before continuing to login. */
    private static final Duration VERSI_BARU_PROBE = Duration.ofSeconds(12);
    /** Max wait after clicking Update Sekarang (typical in-app update ~20–30s; extra margin for CI). */
    private static final Duration VERSI_BARU_UPDATE_MAX = Duration.ofSeconds(45);

    @Override
    protected void preInitDriver(DriverConfig config) {
        Capabilities caps = config.getCapabilities();

        String platformName = nullSafeToString(caps.getCapability("platformName"));
        if (platformName.isBlank()) {
            platformName = nullSafeToString(caps.getCapability("appium:platformName"));
        }
        if (!platformName.equalsIgnoreCase("mac")) {
            return;
        }

        String bundleId = nullSafeToString(caps.getCapability("appium:bundleId"));

        String appPath = nullSafeToString(caps.getCapability("appium:appPath"));
        if (appPath.isBlank()) {
            appPath = nullSafeToString(caps.getCapability("appium:app"));
        }

        if (appPath.isBlank() || !appPath.endsWith(".app")) {
            return;
        }

        File appBundle = new File(appPath);
        if (!appBundle.exists()) {
            return;
        }

        // If the app is already running (common with skipAppKill=true), avoid mutating the bundle
        // while processes are active. This prevents sporadic "app not running / crashed" issues.
        if (!bundleId.isBlank() && isBundleRunning(bundleId)) {
            Allure.addAttachment("mac pre-step: xattr -cr skipped", "App already running for bundleId=" + bundleId, "text/plain");
            return;
        }

        // Clear quarantine/extended attributes so macOS won't block the app launch.
        // This is commonly required for apps copied from DMG downloads.
        try {
            Process p = new ProcessBuilder("xattr", "-cr", appPath)
                    .redirectErrorStream(true)
                    .start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = p.waitFor();
            Allure.addAttachment("mac pre-step: xattr -cr (exit " + code + ")", output, "text/plain");

            if (code != 0) {
                throw new RuntimeException("xattr -cr failed with exit code " + code + ": " + output);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to run xattr -cr for appPath=" + appPath, e);
        }
    }

    private static String nullSafeToString(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    /**
     * Fails fast with a clear message when {@code -Denv=mac} was omitted (default {@code dev} is Android).
     */
    private static void assertMacDesktopConfigOrThrow() {
        Optional<DriverConfig> cfg = DriverConfigLoader.load();
        if (cfg.isEmpty()) {
            throw new IllegalStateException(
                    "No driver classpath config (need appium.server.url + platformName). For this test run: "
                    + "mvn -pl automation -am -Denv=mac -Dtest="
                    + LaunchDesktopAppTest.class.getName()
                    + " test");
        }
        Capabilities caps = cfg.get().getCapabilities();
        String platform = nullSafeToString(caps.getCapability("platformName"));
        if (platform.isBlank()) {
            platform = nullSafeToString(caps.getCapability("appium:platformName"));
        }
        if (platform.equalsIgnoreCase("mac")) {
            return;
        }
        String env = System.getProperty("env", "dev");
        throw new IllegalStateException(
                "LaunchDesktopAppTest needs Mac2 (platformName=Mac), but env=\""
                + env
                + "\" resolved to platformName="
                + (platform.isBlank() ? "(missing)" : platform)
                + ". Default env is Android. Run: mvn -pl automation -am -Denv=mac -Dtest="
                + LaunchDesktopAppTest.class.getName()
                + " test — see docs/DESKTOP_MAC.md");
    }

    private static boolean isBundleRunning(String bundleId) {
        // Use AppleScript to count processes with a given bundle identifier.
        // Example expression returns a number.
        try {
            Process p = new ProcessBuilder(
                    "osascript",
                    "-e",
                    "tell application \"System Events\" to return count of (processes whose bundle identifier is \"" + bundleId + "\")"
            ).redirectErrorStream(true).start();

            if (!p.waitFor(4, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }

            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (out.isBlank()) {
                return false;
            }
            return Integer.parseInt(out) > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Test(groups = "mac", description = "Launch macOS app, verify main window, interact with a UI element")
    public void launchDesktopAppAndInteract() {
        assertMacDesktopConfigOrThrow();
        AppiumDriver driver = getDriver();
        Object currentBundleId = driver.getCapabilities().getCapability("CFBundleIdentifier");
        Allure.addAttachment("mac2:CFBundleIdentifier", String.valueOf(currentBundleId == null ? "" : currentBundleId), "text/plain");

        MainWindowPage mainWindow = new MainWindowPage(driver);
        mainWindow.waitForMainWindowLoaded();

        // Capture right after launch + first UI becomes available.
        attachScreenshot("Mac app launched");

        mainWindow.maximizeWindowBeforeCredentials();

        // Optional in-app update dialog ("Versi Baru Tersedia" → Update Sekarang); skip if not shown.
        mainWindow.handleOptionalVersiBaruAfterLaunch(VERSI_BARU_PROBE, VERSI_BARU_UPDATE_MAX);

        // Enter credentials (email + password) into the app.
        // Avoid clicking an arbitrary "first element" first, since that can trigger navigation
        // (or even crash/close) before the login fields are available.
        String email = "nakesh.merugu14@ajaib.co.id";
        String password = "Ajaib123!";
        try {
            mainWindow.enterCredentials(email, password);
        } catch (org.openqa.selenium.TimeoutException | org.openqa.selenium.NoSuchElementException | org.openqa.selenium.InvalidElementStateException | IllegalStateException e) {
            // Versi Baru modal often appears after the first probe window; retry handler, then credentials.
            attachScreenshot("Mac enterCredentials failed — retry Versi Baru / unblock overlays");
            mainWindow.handleOptionalVersiBaruAfterLaunch(VERSI_BARU_PROBE, VERSI_BARU_UPDATE_MAX);
            try {
                mainWindow.enterCredentials(email, password);
            } catch (org.openqa.selenium.TimeoutException | org.openqa.selenium.NoSuchElementException | org.openqa.selenium.InvalidElementStateException | IllegalStateException e2) {
                attachScreenshot("Mac before fallback click to reach login fields");
                mainWindow.interactWithFirstAvailableElement();
                attachScreenshot("Mac main window after fallback overlay/start click");
                mainWindow.enterCredentials(email, password);
            }
        }

        // Submit via mac2 button nodes (avoid flaky XPath for embedded webviews).
        mainWindow.clickSubmitUsingButtons();
        attachScreenshot("Mac after login submit (credentials entered)");

        String pin = "1234";

        mainWindow.ensurePinStepAfterLoginSubmit();
        mainWindow.enterPinByInputs(pin);
        mainWindow.completePinStep();
        mainWindow.waitForHomeDashboardVisible(HOME_DASHBOARD_WAIT);
        attachScreenshot("Mac home dashboard");

        mainWindow.clickProfileAndLogout();
        //mainWindow.waitForLoginScreenVisible(LOGIN_AFTER_LOGOUT_WAIT);
        //attachScreenshot("Mac back on login screen");

        mainWindow.closeWindowAfterLogin();
        DriverManager.getInstance().quitDriver();
    }

}
