package com.mobile.automation.desktop.mac;

import com.mobile.automation.config.DriverConfig;
import com.mobile.automation.config.DriverConfigLoader;
import com.mobile.automation.desktop.mac.pages.MainWindowPage;
import com.mobile.automation.tests.BaseTest;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.Capabilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Base for Mac2 desktop tests: quarantine clearance before launch, env guard, and small helpers
 * so test methods stay Arrange → Act → Assert.
 */
public abstract class BaseMacDesktopTest extends BaseTest {

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

        if (!bundleId.isBlank() && isBundleRunning(bundleId)) {
            Allure.addAttachment("mac pre-step: xattr -cr skipped", "App already running for bundleId=" + bundleId, "text/plain");
            return;
        }

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

    /**
     * Fails fast when {@code -Denv=mac} was omitted or config is not Mac2.
     */
    protected void requireMacDesktopConfig() {
        Optional<DriverConfig> cfg = DriverConfigLoader.load();
        if (cfg.isEmpty()) {
            throw new IllegalStateException(
                    "No driver classpath config (need appium.server.url + platformName). For Mac desktop tests run: "
                            + "mvn -pl automation -am -Denv=mac -Dtest="
                            + getClass().getName()
                            + " test — see docs/DESKTOP_MAC.md");
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
                "Mac desktop tests need Mac2 (platformName=Mac), but env=\""
                        + env
                        + "\" resolved to platformName="
                        + (platform.isBlank() ? "(missing)" : platform)
                        + ". Run: mvn -pl automation -am -Denv=mac -Dtest="
                        + getClass().getName()
                        + " test — see docs/DESKTOP_MAC.md");
    }

    /** Allure attachment for the active bundle id when the session exposes it. */
    protected void attachMacBundleIdentifier(AppiumDriver driver) {
        Object id = driver.getCapabilities().getCapability("CFBundleIdentifier");
        Allure.addAttachment("mac2:CFBundleIdentifier", String.valueOf(id == null ? "" : id), "text/plain");
    }

    /** Shorthand for {@code new MainWindowPage(getDriver())} when you need the page without a flow. */
    protected MainWindowPage mainWindow() {
        return new MainWindowPage(getDriver());
    }

    private static String nullSafeToString(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static boolean isBundleRunning(String bundleId) {
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
}
