package com.mobile.automation.tests;

import com.mobile.automation.flows.AppReadyFlow;
import org.testng.annotations.Test;

/**
 * Launches the Android app (from config), waits until the app package is visible, takes a screenshot, and closes.
 * Requires: Appium server running, device/emulator connected, and app path set in config (e.g. dev.properties).
 */
public class LaunchAppTest extends BaseTest {

    private static final String APP_PACKAGE = "ajaib.co.dev";
    private static final String VIEW_GROUP = "android.view.ViewGroup";
    private static final String EXISTING_USER_RESOURCE_ID = "ajaib.co.dev:id/vExistUser";
    private static final String NEW_USER_RESOURCE_ID = "ajaib.co.dev:id/vNewUser";
    private static final String EXISTING_USER_XPATH = "//android.view.ViewGroup[@resource-id='ajaib.co.dev:id/vNewUser']";    
    private static final String APP_CLASS_NAME = "android.widget.LinearLayout";
    // //android.widget.LinearLayout[@resource-id="com.android.permissioncontroller:id/grant_dialog"]
    // //android.widget.Button[@resource-id="com.android.permissioncontroller:id/permission_allow_button"]
    private static final String INPUT_EMAIL_RESOURCE_ID = "ajaib.co.dev:id/btnInputEmail";
    // //android.widget.FrameLayout[@resource-id="ajaib.co.dev:id/btnInputEmail"]/android.widget.LinearLayout
    private static final String EDIT_TEXT_CLASS_NAME = "android.widget.EditText";
    // //android.widget.EditText[@resource-id="ajaib.co.dev:id/et"]
    private static final String NEXT_BUTTON_RESOURCE_ID = "ajaib.co.dev:id/btn_next";
    // //android.widget.Button[@resource-id="ajaib.co.dev:id/btn_next"]
    private static final String EDIT_TEXT_PASSWORD_RESOURCE_ID = "ajaib.co.dev:id/textinput_placeholder";
    //Enter passowrd xpath
    // //android.widget.TextView[@resource-id="ajaib.co.dev:id/textinput_placeholder"]
// submit password button xpath
// //android.widget.Button[@resource-id="ajaib.co.dev:id/btnLogin"]

    @Test(description = "Launch app, wait until package and ViewGroup loaded, click element, take screenshot, then close (driver quit in BaseTest @AfterMethod)")
    public void launchAppTakeScreenshotAndClose() throws InterruptedException {
        getDriver();
        AppReadyFlow flow = new AppReadyFlow(getDriver());
        flow.waitUntilPackageVisible(APP_PACKAGE);
        //Thread.sleep(10000);
        //flow.waitUntilViewGroupLoaded(VIEW_GROUP);
        //flow.clickByResourceId(NEW_USER_RESOURCE_ID);
        //flow.clickByResourceId("ajaib.co.dev:id/ivNewUser");
        Thread.sleep(1000);
        flow.clickByXPath(EXISTING_USER_XPATH);
        //flow.clickByResourceId("ajaib.co.dev:id/tvNewUserTitle");
        Thread.sleep(1000);
        flow.allowNotificationPermission();
        Thread.sleep(1000);
        flow.clickByResourceId(INPUT_EMAIL_RESOURCE_ID);
        flow.sendKeysByClassName(EDIT_TEXT_CLASS_NAME, "nakesh.merugu14@ajaib.co.id");
        flow.clickByResourceId(NEXT_BUTTON_RESOURCE_ID);
        Thread.sleep(100);
        flow.sendKeysByXPath("//android.widget.TextView[@resource-id='ajaib.co.dev:id/textinput_placeholder']", "Ajaib123!");
        flow.clickByXPath("//android.widget.TextView[@resource-id='ajaib.co.dev:id/textinput_placeholder']");
        Thread.sleep(1000);
        flow.clickByResourceId("ajaib.co.dev:id/btn_another_way");
        flow.clickByResourceId("ajaib.co.dev:id/ivToggle");
        flow.sendKeysByResourceId("ajaib.co.dev:id/etSearch", "INDRAGIRI HULU");
        flow.clickByResourceId("ajaib.co.dev:id/text_input_end_icon");
        flow.clickByResourceId("ajaib.co.dev:id/tvItem");
        flow.clickByResourceId("ajaib.co.dev:id/btn_submit");
        flow.clickByResourceId("ajaib.co.dev:id/btnNumPad1");
        // //android.widget.TextView[@resource-id="ajaib.co.dev:id/btnNumPad1"]
        flow.clickByResourceId("ajaib.co.dev:id/btnNumPad2");
        // //android.widget.TextView[@resource-id="ajaib.co.dev:id/btnNumPad2"]
        flow.clickByResourceId("ajaib.co.dev:id/btnNumPad3");
        // //android.widget.TextView[@resource-id="ajaib.co.dev:id/btnNumPad3"]
        flow.clickByResourceId("ajaib.co.dev:id/btnNumPad4");
        // //android.widget.TextView[@resource-id="ajaib.co.dev:id/btnNumPad4"]
        Thread.sleep(20000);
        attachScreenshot("App launched with view");
    }
}
