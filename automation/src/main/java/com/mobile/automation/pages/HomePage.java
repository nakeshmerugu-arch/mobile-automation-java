package com.mobile.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * First screen after app launch. Element interaction only; no business or test logic.
 * Uses WaitUtils for explicit waits (no Thread.sleep()).
 */
public class HomePage extends BasePage {

    /** Android: standard content frame for the current activity. */
    private static final By CONTENT = By.id("android:id/content");

    /** Android: ViewGroup class; wait for it to confirm the view hierarchy is loaded. */
    private static final By VIEW_GROUP = By.className("android.view.ViewGroup");

    public HomePage(AppiumDriver driver) {
        super(driver);
    }

    /**
     * Waits until the main content is visible. Use to confirm the app has finished loading.
     */
    public void waitUntilContentVisible() {
        waitUtils().waitUntilVisible(CONTENT);
    }

    /**
     * Waits until the given app package is in the foreground (Android). Use to confirm the app window is visible.
     *
     * @param packageName app package (e.g. ajaib.co.dev)
     */
    public void waitUntilPackageVisible(String packageName) {
        waitUtils().waitUntilPackageInForeground(packageName);
    }

    /**
     * Waits until at least one android.view.ViewGroup is visible. Use as a follow-up to confirm the view hierarchy is loaded.
     */
    public void waitUntilViewGroupLoaded() {
        waitUtils().waitUntilVisible(VIEW_GROUP);
    }

    /**
     * Waits until at least one element with the given class name is visible (e.g. android.view.ViewGroup).
     */
    public void waitUntilViewGroupLoaded(String viewGroupClassName) {
        waitUtils().waitUntilVisible(By.className(viewGroupClassName));
    }

    /**
     * Waits until the element matching the locator is clickable and clicks it. Generic entry point for any By.
     */
    public void clickBy(By locator) {
        waitUtils().waitUntilClickable(locator).click();
    }

    /**
     * Clicks the element with the given Android resource id (waits until clickable first).
     *
     * @param resourceId full resource id (e.g. ajaib.co.dev:id/vExistUser) or short id
     */
    public void clickByResourceId(String resourceId) {
        clickBy(By.id(resourceId));
    }

    /**
     * Clicks the element matching the given XPath (waits until clickable first).
     *
     * @param xpath XPath expression (e.g. //android.widget.Button[@text='Login'])
     */
    public void clickByXPath(String xpath) {
        clickBy(By.xpath(xpath));
    }

    /**
     * Clicks the first element with the given class name (waits until clickable first).
     *
     * @param className full class name (e.g. android.widget.Button)
     */
    public void clickByClassName(String className) {
        clickBy(By.className(className));
    }

    /**
     * Clicks the element with the given accessibility id (content-desc on Android, accessibilityIdentifier on iOS).
     *
     * @param accessibilityId accessibility id value
     */
    public void clickByAccessibilityId(String accessibilityId) {
        clickBy(AppiumBy.accessibilityId(accessibilityId));
    }

    /**
     * Clicks the element with the given visible text (Android only; uses UiSelector).
     *
     * @param text exact visible text or content description
     */
    public void clickByText(String text) {
        clickBy(AppiumBy.androidUIAutomator("new UiSelector().text(\"" + text.replace("\"", "\\\"") + "\")"));
    }

    /**
     * Clicks "Allow" on the system notification permission popup (Android). Waits until the button is clickable.
     */
    public void clickAllowOnNotificationPopup() {
        clickByText("Allow");
    }

    /**
     * Sends keys to the element matching the locator (waits until visible, then clear and sendKeys).
     */
    public void sendKeys(By locator, CharSequence keys) {
        WebElement el = waitUtils().waitUntilVisible(locator);
        el.clear();
        el.sendKeys(keys);
    }

    /**
     * Sends keys to the element with the given resource id.
     */
    public void sendKeysByResourceId(String resourceId, CharSequence keys) {
        sendKeys(By.id(resourceId), keys);
    }

    /**
     * Sends keys to the first element with the given class name (e.g. android.widget.EditText).
     */
    public void sendKeysByClassName(String className, CharSequence keys) {
        sendKeys(By.className(className), keys);
    }

    /**
     * Sends keys to the element matching the given XPath.
     */
    public void sendKeysByXPath(String xpath, CharSequence keys) {
        sendKeys(By.xpath(xpath), keys);
    }
}
