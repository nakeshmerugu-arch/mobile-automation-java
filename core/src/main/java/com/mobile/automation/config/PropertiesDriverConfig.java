package com.mobile.automation.config;

import io.appium.java_client.remote.options.BaseOptions;
import org.openqa.selenium.Capabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds {@link DriverConfig} from a {@link Properties} instance (e.g. loaded from classpath config).
 * Required keys: appium.server.url, platformName. Other keys are set as capabilities.
 */
public final class PropertiesDriverConfig implements DriverConfig {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesDriverConfig.class);

    public static final String KEY_APPIUM_SERVER_URL = "appium.server.url";
    public static final String KEY_PLATFORM_NAME = "platformName";

    private final String appiumServerUrl;
    private final Capabilities capabilities;

    /**
     * Builds config from the given properties. Required: {@value #KEY_APPIUM_SERVER_URL}, {@value #KEY_PLATFORM_NAME}.
     * Any other property is set as a capability (key as-is). Values are trimmed.
     */
    public PropertiesDriverConfig(Properties properties) {
        if (properties == null || properties.isEmpty()) {
            throw new IllegalArgumentException("Properties must not be null or empty");
        }
        this.appiumServerUrl = requireNonBlank(properties.getProperty(KEY_APPIUM_SERVER_URL), KEY_APPIUM_SERVER_URL);
        this.capabilities = buildCapabilities(properties);
    }

    @Override
    public String getAppiumServerUrl() {
        return appiumServerUrl;
    }

    @Override
    public Capabilities getCapabilities() {
        return capabilities;
    }

    private static String requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing or blank property: " + key);
        }
        return value.trim();
    }

    private static Capabilities buildCapabilities(Properties properties) {
        BaseOptions<?> options = new BaseOptions<>();
        Set<String> skip = Set.of(KEY_APPIUM_SERVER_URL, "environment", "env");
        Set<String> capabilityKeys = properties.stringPropertyNames().stream()
                .filter(k -> !skip.contains(k))
                .collect(Collectors.toSet());

        for (String key : capabilityKeys) {
            String value = properties.getProperty(key);
            if (value != null && !value.isBlank()) {
                value = value.trim();
                if ("app".equals(key)) {
                    value = toAbsoluteAppPath(value);
                }
                options.setCapability(key, toCapabilityValue(value));
            }
        }

        if (options.getCapability("platformName") == null) {
            throw new IllegalArgumentException("Missing capability: " + KEY_PLATFORM_NAME);
        }
        LOG.debug("Built capabilities from {} keys", capabilityKeys.size());
        return options;
    }

    /**
     * Resolves the app path to absolute so the Appium server can find the file regardless of its working directory.
     * Relative paths are resolved against the current process working directory (e.g. automation module when run via Maven).
     */
    private static Object toCapabilityValue(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return value;
    }

    private static String toAbsoluteAppPath(String path) {
        Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return path;
        }
        String resolved = Paths.get(System.getProperty("user.dir", ".")).resolve(path).normalize().toAbsolutePath().toString();
        LOG.debug("Resolved app path to absolute: {}", resolved);
        return resolved;
    }
}
