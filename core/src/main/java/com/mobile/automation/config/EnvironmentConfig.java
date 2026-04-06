package com.mobile.automation.config;

/**
 * Contract for environment-driven configuration.
 * Implementation will load from properties/JSON based on active environment (Phase 2/3).
 * No hardcoded credentials, URLs, or device IDs.
 */
public interface EnvironmentConfig {

    /**
     * Active environment name (e.g. dev, stg, prod).
     */
    String getEnvironment();
}
