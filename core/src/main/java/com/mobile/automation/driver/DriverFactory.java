package com.mobile.automation.driver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.mac.Mac2Driver;
import org.openqa.selenium.Capabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Creates Appium driver instances by platform. Used by {@link DriverManager}; tests do not instantiate drivers directly.
 * No static driver; each call creates a new driver bound to the given server URL and capabilities.
 */
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);

    /** Standard W3C / Appium capability for platform name. */
    public static final String CAPABILITY_PLATFORM_NAME = "platformName";

    private static final String PLATFORM_ANDROID = "Android";
    private static final String PLATFORM_IOS = "iOS";
    private static final String PLATFORM_MAC = "Mac";

    private DriverFactory() {
        // utility-style factory; no instance state
    }

    /**
     * Creates an Appium driver for the platform specified in capabilities (platformName/platform: Android, iOS, or Mac).
     *
     * @param serverUrl   Appium server URL (e.g. http://127.0.0.1:4723). Must not be null.
     * @param capabilities session capabilities; must include platformName (Android or iOS).
     * @return new AppiumDriver instance (AndroidDriver or IOSDriver)
     * @throws IllegalArgumentException if serverUrl or capabilities is null, or platformName is missing/unsupported
     */
    public static AppiumDriver createDriver(URL serverUrl, Capabilities capabilities) {
        if (serverUrl == null) {
            throw new IllegalArgumentException("Appium server URL must not be null");
        }
        if (capabilities == null) {
            throw new IllegalArgumentException("Capabilities must not be null");
        }

        String platformName = normalizePlatformName(getPlatformName(capabilities));
        LOG.debug("Creating driver for platform: {}, server: {}", platformName, serverUrl);

        return switch (platformName) {
            case PLATFORM_ANDROID -> new AndroidDriver(serverUrl, capabilities);
            case PLATFORM_IOS -> new IOSDriver(serverUrl, capabilities);
            case PLATFORM_MAC -> new Mac2Driver(serverUrl, capabilities);
            default -> throw new IllegalArgumentException(
                    "Unsupported platform/platformName: " + platformName + ". Use "
                            + PLATFORM_ANDROID + ", " + PLATFORM_IOS + " or " + PLATFORM_MAC);
        };
    }

    private static String getPlatformName(Capabilities capabilities) {
        Object raw = capabilities.getCapability(CAPABILITY_PLATFORM_NAME);
        if (raw == null) {
            raw = capabilities.getCapability("appium:platformName");
        }
        if (raw == null) {
            // Allow generic "platform=android|ios|mac" style flags as a fallback.
            raw = capabilities.getCapability("platform");
        }
        if (raw == null) {
            throw new IllegalArgumentException(
                    "Capabilities must include platform/platformName (or appium:platformName)");
        }
        return raw.toString().trim();
    }

    private static String normalizePlatformName(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (PLATFORM_ANDROID.equalsIgnoreCase(value)) {
            return PLATFORM_ANDROID;
        }
        if (PLATFORM_IOS.equalsIgnoreCase(value)) {
            return PLATFORM_IOS;
        }
        if (PLATFORM_MAC.equalsIgnoreCase(value) || "macos".equalsIgnoreCase(value)) {
            return PLATFORM_MAC;
        }
        return value;
    }
}
