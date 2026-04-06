package com.mobile.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Placeholder test to verify Phase 1–3 setup: build, TestNG, Allure, and BaseTest lifecycle.
 * Does not require a real Appium server (config has no appium.server.url). Remove or repurpose when real tests are added.
 */
public class FrameworkReadyTest extends BaseTest {

    @Test(description = "Framework compiles, test runtime and BaseTest lifecycle ready")
    public void frameworkReady() {
        Assert.assertTrue(true, "Framework setup verified");
    }
}
