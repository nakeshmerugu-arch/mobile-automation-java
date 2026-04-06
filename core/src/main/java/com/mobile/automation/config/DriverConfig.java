package com.mobile.automation.config;

import org.openqa.selenium.Capabilities;

/**
 * Supplies Appium server URL and session capabilities for driver creation.
 * Implementations must be environment-driven (e.g. from properties/JSON); no hardcoded URLs or credentials.
 */
public interface DriverConfig {

    /**
     * Appium server URL (e.g. http://127.0.0.1:4723). Must not be null or blank.
     */
    String getAppiumServerUrl();

    /**
     * Session capabilities including at least platformName (Android or iOS).
     */
    Capabilities getCapabilities();
}
