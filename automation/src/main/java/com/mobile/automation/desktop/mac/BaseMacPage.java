package com.mobile.automation.desktop.mac;

import com.mobile.automation.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.mac.Mac2Driver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Base page for macOS desktop automation.
 * Extends the generic BasePage but exposes Mac2Driver-specific helpers when needed.
 */
public abstract class BaseMacPage extends BasePage {

    protected BaseMacPage(AppiumDriver driver) {
        super(driver);
    }

    protected Mac2Driver macDriver() {
        return (Mac2Driver) getDriver();
    }

    /**
     * Switches to a window by name (e.g. main application window title).
     */
    protected void switchToWindow(String windowName) {
        macDriver().switchTo().window(windowName);
    }

    /**
     * Clicks a menu item from the macOS menu bar.
     *
     * @param menuBarName  top-level menu (e.g. "File")
     * @param menuItemName submenu item (e.g. "Open")
     */
    protected void clickMenuItem(String menuBarName, String menuItemName) {
        macDriver().findElement(By.name(menuBarName)).click();
        macDriver().findElement(By.name(menuItemName)).click();
    }

    /**
     * Clicks a system dialog button by its label (e.g. "OK", "Cancel", "Save").
     */
    protected void clickSystemDialogButton(String buttonLabel) {
        macDriver().findElement(By.name(buttonLabel)).click();
    }

    protected void typeIntoField(By locator, String value) {
        WebElement el = waitUtils().waitUntilVisible(locator);
        el.click();
        try {
            el.clear();
        } catch (Exception ignored) {
            // Some mac2 elements might not support clear; sendKeys will append otherwise.
        }
        el.sendKeys(value);
    }

    /**
     * Best-effort maximize the application window (Selenium window API; works for many Mac2 sessions).
     */
    protected void maximizeMainWindow() {
        try {
            getDriver().manage().window().maximize();
        } catch (Exception ignored) {
        }
    }

    /**
     * Closes the frontmost app window using macOS Cmd+W.
     * (Avoids WebDriver {@code close()} which can hang in mac2 sessions.)
     */
    protected void closeMainWindow() {
        try {
            new Actions(getDriver())
                    .keyDown(Keys.COMMAND)
                    .sendKeys("w")
                    .keyUp(Keys.COMMAND)
                    .perform();
        } catch (Exception ignored) {
        }
    }
}

