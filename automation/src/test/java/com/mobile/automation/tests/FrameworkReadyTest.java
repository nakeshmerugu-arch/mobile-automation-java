package com.mobile.automation.tests;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke test: build, TestNG, Allure, BaseTest lifecycle. Does not call {@code getDriver()}, so no Appium session is
 * started and no device/APK is required.
 */
public class FrameworkReadyTest extends BaseTest {

    @Test(description = "Framework compiles, test runtime and BaseTest lifecycle ready")
    public void frameworkReady() {
        Assert.assertTrue(true, "Framework setup verified");
    }
}
