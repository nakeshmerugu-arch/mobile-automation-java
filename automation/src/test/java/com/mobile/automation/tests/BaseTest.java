package com.mobile.automation.tests;

import com.mobile.automation.config.DriverConfig;
import com.mobile.automation.config.DriverConfigLoader;
import com.mobile.automation.driver.DriverManager;
import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * Base class for tests. Handles driver lifecycle (init from config in @BeforeMethod, quit in @AfterMethod)
 * and attaches screenshot on failure. Tests must not initialize the driver; they use {@link #getDriver()}
 * when they need it (e.g. to pass to pages/flows).
 */
public abstract class BaseTest {
    /**
     * Debug flag: when enabled, we do not quit the driver in @AfterMethod so you can visually inspect the app.
     * Enable with: -Dkeep.driver.alive=true
     */
    private boolean shouldKeepDriverAlive() {
        return Boolean.parseBoolean(System.getProperty("keep.driver.alive", "false"));
    }

    /**
     * Initializes the driver when config is available (appium.server.url and platformName set).
     * When config is incomplete, driver is not created so tests that do not need a device can still run.
     */
    @BeforeMethod(alwaysRun = true)
    public void initDriverIfConfigured() {
        Optional<DriverConfig> config = DriverConfigLoader.load();
        config.ifPresent(c -> {
            preInitDriver(c);
            DriverManager.getInstance().initDriver(c.getAppiumServerUrl(), c.getCapabilities());
        });
    }

    /**
     * Optional hook for platform-specific prerequisites that must happen before the driver starts.
     * Subclasses can override (e.g., macOS quarantine clearance before launching the app).
     */
    protected void preInitDriver(DriverConfig config) {
        // default no-op
    }

    /**
     * Quits the driver and attaches a screenshot on failure. No reporting logic inside test methods.
     */
    @AfterMethod(alwaysRun = true)
    public void quitDriverAndAttachScreenshotOnFailure(ITestResult result) {
        DriverManager manager = DriverManager.getInstance();
        if (manager.hasDriver()) {
            if (result.getStatus() == ITestResult.FAILURE) {
                attachScreenshotOnFailure(manager.getDriver());
            }
            if (!shouldKeepDriverAlive()) {
                manager.quitDriver();
            }
        }
    }

    /**
     * Returns the current thread's driver. Use when building pages or flows. Throws if no driver was initialized
     * (e.g. config was incomplete).
     */
    protected AppiumDriver getDriver() {
        return DriverManager.getInstance().getDriver();
    }

    /**
     * Takes a screenshot of the current driver and attaches it to Allure. Use for on-demand screenshots in tests.
     *
     * @param attachmentName name shown in the Allure report (e.g. "App launched")
     */
    protected void attachScreenshot(String attachmentName) {
        if (!DriverManager.getInstance().hasDriver()) {
            return;
        }
        attachScreenshotToAllure(DriverManager.getInstance().getDriver(), attachmentName);
    }

    private static void attachScreenshotOnFailure(AppiumDriver driver) {
        attachScreenshotToAllure(driver, "Screenshot on failure");
    }

    private static void attachScreenshotToAllure(AppiumDriver driver, String attachmentName) {
        if (driver instanceof TakesScreenshot) {
            try {
                byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment(attachmentName, "image/png", new ByteArrayInputStream(bytes), "png");
            } catch (Exception e) {
                Allure.addAttachment(attachmentName + " (error)", "text/plain", e.getMessage());
            }
        }
    }
}
