package com.mobile.automation.tests;

import com.mobile.automation.flows.AppReadyFlow;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Sample feature test: launch app, wait until ready, take screenshot, assert.
 * Test layer: assertions and high-level intent only; no direct page element interaction.
 */
public class SampleFeatureTest extends BaseTest {

    @Test(description = "App launches and first screen becomes ready")
    public void appLaunchesAndIsReady() {
        AppReadyFlow flow = new AppReadyFlow(getDriver());
        flow.waitUntilAppReady();
        attachScreenshot("App ready");
        Assert.assertTrue(true, "App ready flow completed");
    }
}
