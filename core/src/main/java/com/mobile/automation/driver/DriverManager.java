package com.mobile.automation.driver;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Capabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Thread-local driver holder. Lifecycle is intended to be driven by BaseTest: init in @BeforeMethod, quit in @AfterMethod.
 * No static driver; each thread has its own driver instance.
 */
public final class DriverManager {

    private static final Logger LOG = LoggerFactory.getLogger(DriverManager.class);

    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
        // singleton holder; use getInstance() for the manager
    }

    /**
     * Returns the single DriverManager instance (manager is singleton; the driver is per-thread).
     */
    public static DriverManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Returns the driver for the current thread. Must call {@link #initDriver(URL, Capabilities)} (or
     * {@link #setDriver(AppiumDriver)}) first in this thread.
     *
     * @return the current thread's AppiumDriver
     * @throws IllegalStateException if no driver has been set for this thread
     */
    public AppiumDriver getDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No driver for current thread. Call initDriver() or setDriver() (e.g. from BaseTest @BeforeMethod) first.");
        }
        return driver;
    }

    /**
     * Sets the driver for the current thread. Prefer {@link #initDriver(URL, Capabilities)} so the factory creates it.
     *
     * @param driver driver instance for this thread; may be null to clear (after quit)
     */
    public void setDriver(AppiumDriver driver) {
        DRIVER.set(driver);
    }

    /**
     * Creates a new driver via {@link DriverFactory} and sets it for the current thread.
     *
     * @param serverUrl    Appium server URL (must not be null)
     * @param capabilities session capabilities including platformName (must not be null)
     * @return the newly created driver
     */
    public AppiumDriver initDriver(URL serverUrl, Capabilities capabilities) {
        quitDriver();
        AppiumDriver driver = DriverFactory.createDriver(serverUrl, capabilities);
        setDriver(driver);
        LOG.debug("Driver initialized for thread: {}", Thread.currentThread().getName());
        return driver;
    }

    /**
     * Creates a new driver using the given server URL string and sets it for the current thread.
     *
     * @param serverUrl    Appium server URL (e.g. http://127.0.0.1:4723)
     * @param capabilities session capabilities including platformName
     * @return the newly created driver
     * @throws IllegalArgumentException if serverUrl is malformed
     */
    public AppiumDriver initDriver(String serverUrl, Capabilities capabilities) {
        try {
            return initDriver(new URL(serverUrl), capabilities);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Appium server URL: " + serverUrl, e);
        }
    }

    /**
     * Quits the current thread's driver (if any) and clears the thread-local reference.
     * Safe to call when no driver is set.
     */
    public void quitDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
                LOG.debug("Driver quit for thread: {}", Thread.currentThread().getName());
            } catch (Exception e) {
                LOG.warn("Error quitting driver: {}", e.getMessage());
            } finally {
                DRIVER.remove();
            }
        }
    }

    /**
     * Whether the current thread has a driver set (may or may not be quit).
     */
    public boolean hasDriver() {
        return DRIVER.get() != null;
    }

    private static final class Holder {
        private static final DriverManager INSTANCE = new DriverManager();
    }
}
