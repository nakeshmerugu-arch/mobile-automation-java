package com.mobile.automation.flows;

import com.mobile.automation.pages.HomePage;
import io.appium.java_client.AppiumDriver;

/**
 * Business-level flow: ensure the app has launched and its first screen is ready.
 * Composes pages; no direct element interaction.
 */
public class AppReadyFlow {

    private final AppiumDriver driver;

    public AppReadyFlow(AppiumDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver must not be null");
        }
        this.driver = driver;
    }

    /**
     * Waits until the app home/content is visible. Returns when the first screen is ready.
     */
    public void waitUntilAppReady() {
        new HomePage(driver).waitUntilContentVisible();
    }

    /**
     * Waits until the given app package is in the foreground (Android). Use to confirm the app window is visible.
     *
     * @param packageName app package (e.g. ajaib.co.dev)
     */
    public void waitUntilPackageVisible(String packageName) {
        new HomePage(driver).waitUntilPackageVisible(packageName);
    }

    /**
     * Waits until android.view.ViewGroup is loaded (visible). Use after package is visible to ensure the view hierarchy is ready.
     */
    public void waitUntilViewGroupLoaded(String viewGroup) {
        new HomePage(driver).waitUntilViewGroupLoaded(viewGroup);
    }

    /**
     * Clicks the element with the given Android resource id (waits until clickable first).
     *
     * @param resourceId full resource id (e.g. ajaib.co.dev:id/vExistUser)
     */
    public void clickByResourceId(String resourceId) {
        new HomePage(driver).clickByResourceId(resourceId);
    }

    /**
     * Clicks the element matching the given XPath (waits until clickable first).
     *
     * @param xpath XPath expression
     */
    public void clickByXPath(String xpath) {
        new HomePage(driver).clickByXPath(xpath);
    }

    /**
     * Clicks the first element with the given class name (waits until clickable first).
     *
     * @param className full class name (e.g. android.widget.Button)
     */
    public void clickByClassName(String className) {
        new HomePage(driver).clickByClassName(className);
    }

    /**
     * Clicks the element with the given accessibility id (content-desc / accessibilityIdentifier).
     *
     * @param accessibilityId accessibility id value
     */
    public void clickByAccessibilityId(String accessibilityId) {
        new HomePage(driver).clickByAccessibilityId(accessibilityId);
    }

    /**
     * Clicks the element with the given visible text (Android only).
     *
     * @param text exact visible text
     */
    public void clickByText(String text) {
        new HomePage(driver).clickByText(text);
    }

    /**
     * Clicks "Allow" on the system notification permission popup. Call after an action that triggers the dialog.
     */
    public void allowNotificationPermission() {
        new HomePage(driver).clickAllowOnNotificationPopup();
    }

    /**
     * Sends keys to the element with the given resource id (waits until visible, clear, then sendKeys).
     */
    public void sendKeysByResourceId(String resourceId, CharSequence keys) {
        new HomePage(driver).sendKeysByResourceId(resourceId, keys);
    }

    /**
     * Sends keys to the first element with the given class name (e.g. android.widget.EditText).
     */
    public void sendKeysByClassName(String className, CharSequence keys) {
        new HomePage(driver).sendKeysByClassName(className, keys);
    }

    /**
     * Sends keys to the element matching the given XPath.
     */
    public void sendKeysByXPath(String xpath, CharSequence keys) {
        new HomePage(driver).sendKeysByXPath(xpath, keys);
    }
}
