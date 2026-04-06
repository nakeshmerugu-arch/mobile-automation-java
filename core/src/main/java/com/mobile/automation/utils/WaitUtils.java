package com.mobile.automation.utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Centralized explicit waits. Uses WebDriverWait only; no Thread.sleep() or implicit waits.
 * Use from page classes when waiting for elements before interaction.
 */
public final class WaitUtils {

    private static final Logger LOG = LoggerFactory.getLogger(WaitUtils.class);

    /** Default wait timeout (seconds). */
    public static final int DEFAULT_TIMEOUT_SECONDS = 15;

    /** Default polling interval (milliseconds). */
    public static final long DEFAULT_POLL_INTERVAL_MS = 500;

    private final WebDriver driver;
    private final Duration timeout;
    private final Duration pollInterval;

    /**
     * Creates WaitUtils with the given driver and default timeout/poll interval.
     */
    public WaitUtils(WebDriver driver) {
        this(driver, Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS), Duration.ofMillis(DEFAULT_POLL_INTERVAL_MS));
    }

    /**
     * Creates WaitUtils with the given driver and timeout; default poll interval.
     */
    public WaitUtils(WebDriver driver, Duration timeout) {
        this(driver, timeout, Duration.ofMillis(DEFAULT_POLL_INTERVAL_MS));
    }

    /**
     * Creates WaitUtils with the given driver, timeout, and poll interval.
     */
    public WaitUtils(WebDriver driver, Duration timeout, Duration pollInterval) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver must not be null");
        }
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.driver = driver;
        this.timeout = timeout;
        this.pollInterval = pollInterval != null && !pollInterval.isNegative() ? pollInterval : Duration.ofMillis(DEFAULT_POLL_INTERVAL_MS);
    }

    private FluentWait<WebDriver> createWait() {
        return new WebDriverWait(driver, timeout).pollingEvery(pollInterval);
    }

    /**
     * Waits until the element matching the locator is present in the DOM.
     *
     * @return the element once present
     */
    public WebElement waitUntilPresent(By locator) {
        LOG.debug("Waiting for element present: {}", locator);
        return createWait().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Waits until the element matching the locator is visible (displayed and has size).
     *
     * @return the element once visible
     */
    public WebElement waitUntilVisible(By locator) {
        LOG.debug("Waiting for element visible: {}", locator);
        return createWait().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits until the element matching the locator is clickable (visible and enabled).
     *
     * @return the element once clickable
     */
    public WebElement waitUntilClickable(By locator) {
        LOG.debug("Waiting for element clickable: {}", locator);
        return createWait().until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Waits until the element is no longer attached to the DOM or not visible.
     */
    public void waitUntilInvisible(By locator) {
        LOG.debug("Waiting for element invisible: {}", locator);
        createWait().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Waits until the given element is visible.
     *
     * @return the same element once visible
     */
    public WebElement waitUntilVisible(WebElement element) {
        LOG.debug("Waiting for element visible");
        return createWait().until(ExpectedConditions.visibilityOf(element));
    }

    /**
     * Waits until the given element is clickable.
     *
     * @return the same element once clickable
     */
    public WebElement waitUntilClickable(WebElement element) {
        LOG.debug("Waiting for element clickable");
        return createWait().until(ExpectedConditions.elementToBeClickable(element));
    }

    /**
     * Waits until the given app package is in the foreground (Android only).
     * Uses the current activity package from the driver.
     *
     * @param packageName expected package (e.g. ajaib.co.dev)
     * @throws IllegalArgumentException if driver is not an AndroidDriver
     */
    public void waitUntilPackageInForeground(String packageName) {
        if (!(driver instanceof AndroidDriver)) {
            throw new IllegalArgumentException("waitUntilPackageInForeground is supported only with AndroidDriver");
        }
        LOG.debug("Waiting for package in foreground: {}", packageName);
        createWait().until(d -> packageName.equals(((AndroidDriver) d).getCurrentPackage()));
    }
}
