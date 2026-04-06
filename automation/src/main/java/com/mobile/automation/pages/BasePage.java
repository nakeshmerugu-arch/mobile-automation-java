package com.mobile.automation.pages;

import com.mobile.automation.utils.WaitUtils;
import io.appium.java_client.AppiumDriver;

/**
 * Base for page objects. Holds the driver for element interaction only; no business or test logic.
 * Subclasses receive the driver via constructor and use it for locators and interactions.
 * Use {@link #waitUtils()} for explicit waits (no Thread.sleep()).
 */
public abstract class BasePage {

    private final AppiumDriver driver;
    private WaitUtils waitUtils;

    protected BasePage(AppiumDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver must not be null");
        }
        this.driver = driver;
    }

    /**
     * Returns the driver for element interaction. Use in subclasses for findElement, waits, etc.
     */
    protected AppiumDriver getDriver() {
        return driver;
    }

    /**
     * Returns WaitUtils for explicit waits (visible, clickable, present). Centralized; no Thread.sleep().
     */
    protected WaitUtils waitUtils() {
        if (waitUtils == null) {
            waitUtils = new WaitUtils(driver);
        }
        return waitUtils;
    }
}
