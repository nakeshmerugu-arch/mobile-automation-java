package com.mobile.automation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * Loads driver config from classpath properties. Used by BaseTest to obtain URL and capabilities
 * without hardcoding. Merges default and environment-specific properties.
 */
public final class DriverConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(DriverConfigLoader.class);

    private static final String CONFIG_DEFAULT = "config/default.properties";
    private static final String CONFIG_ENV = "config/%s.properties";
    private static final String ENV_PROPERTY = "env";
    private static final String DEFAULT_ENV = "dev";

    private DriverConfigLoader() {}

    /**
     * Loads config from config/default.properties and config/{env}.properties (env from system property "env" or "dev").
     * Returns empty if required keys (appium.server.url, platformName) are missing so tests can run without a driver.
     */
    public static Optional<DriverConfig> load() {
        Properties merged = new Properties();
        loadResource(CONFIG_DEFAULT, merged);
        String env = System.getProperty(ENV_PROPERTY, DEFAULT_ENV);
        loadResource(String.format(CONFIG_ENV, env), merged);
        return buildConfig(merged);
    }

    private static void loadResource(String path, Properties into) {
        try (InputStream in = DriverConfigLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                into.load(in);
            }
        } catch (Exception e) {
            LOG.debug("Could not load config resource: {} - {}", path, e.getMessage());
        }
    }

    private static Optional<DriverConfig> buildConfig(Properties merged) {
        String url = merged.getProperty(PropertiesDriverConfig.KEY_APPIUM_SERVER_URL);
        String platform = merged.getProperty(PropertiesDriverConfig.KEY_PLATFORM_NAME);
        if (url == null || url.isBlank() || platform == null || platform.isBlank()) {
            LOG.debug("Driver config incomplete (appium.server.url or platformName missing); skipping driver init");
            return Optional.empty();
        }
        try {
            return Optional.of(new PropertiesDriverConfig(merged));
        } catch (Exception e) {
            LOG.warn("Failed to build DriverConfig: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
