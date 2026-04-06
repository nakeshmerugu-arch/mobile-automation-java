package com.mobile.automation.desktop.mac.pages;

import com.mobile.automation.desktop.mac.BaseMacPage;
import com.mobile.automation.utils.WaitUtils;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.Keys;
import io.qameta.allure.Allure;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.time.Duration;

/**
 * Example page object for the main window of a macOS desktop application.
 * Locators are examples; adjust names/identifiers to match your app.
 */
public class MainWindowPage extends BaseMacPage {

    /** Max time to poll for PIN screen / text fields after login submit. */
    private static final Duration PIN_STEP_PROBE_TIMEOUT = Duration.ofSeconds(5);
    /** Poll interval while waiting for PIN inputs (keep small; avoid hammering mac2). */
    private static final Duration PIN_STEP_POLL_INTERVAL = Duration.ofMillis(100);
    /** After PIN submit / Enter: wait for home or PIN UI to go away. */
    private static final Duration PIN_RESOLVE_TIMEOUT = Duration.ofSeconds(15);
    private static final long PIN_RESOLVE_POLL_MS = 150L;
    /** Min interval between full {@code getPageSource()} calls for PIN-in-source checks (mac2 XML is large). */
    private static final long PIN_PAGE_SOURCE_THROTTLE_MS = 300L;
    /** Min interval between home-marker scans in page source (PIN flow must not call this every poll). */
    private static final long HOME_PAGE_SOURCE_THROTTLE_MS = 300L;
    /** Min interval for "Versi Baru" text probes in page source during dialog polling. */
    private static final long VERSI_BARU_PAGE_SOURCE_THROTTLE_MS = 300L;
    /** Post-login home hints exposed in the accessibility tree (no {@code getPageSource}). */
    private static final String[] HOME_UI_MARKERS =
            new String[] { "Market", "Watchlist", "Portfolio", "Trending", "Movers", "Papan Khusus" };
    /** Native profile control: Accessibility Inspector shows type Pop Up Button, title {@code Profile Profile}. */
    private static final By POP_UP_BUTTONS = By.className("XCUIElementTypePopUpButton");
    /** Web/HTML logout fallback when mac2 exposes the popover as DOM buttons. */
    private static final String LOGOUT_BUTTON_XPATH =
            "//button[normalize-space(.)='Logout' or contains(normalize-space(.),'Logout')]";
    private static final By LOGOUT_WEB_BUTTON = By.xpath(LOGOUT_BUTTON_XPATH);
    /** Poll while waiting for home/dashboard markers after PIN. */
    private static final Duration HOME_DASHBOARD_POLL_INTERVAL = Duration.ofMillis(200);
    /** Web button label in "Versi Baru Tersedia" dialog (primary action). */
    private static final String UPDATE_SEKARANG_XPATH =
            "//button[contains(normalize-space(.),'Update Sekarang')]";
    private static final String NANTI_DULU_XPATH =
            "//button[contains(normalize-space(.),'Nanti Dulu')]";

    // Generic locators for mac2/XCUIElementType trees.
    // Using generic types makes the test robust until we capture real accessibility identifiers.
    private static final By STATIC_TEXTS = By.className("XCUIElementTypeStaticText");
    private static final By BUTTONS = By.className("XCUIElementTypeButton");
    // Some apps (or embedded web UIs) render buttons as "Other" or other non-button nodes.
    private static final By OTHER_NODES = By.className("XCUIElementTypeOther");
    private static final By TEXT_FIELDS = By.className("XCUIElementTypeTextField");
    private static final By SECURE_TEXT_FIELDS = By.className("XCUIElementTypeSecureTextField");

    /** Throttle expensive {@link #isPinStepVisibleBySource()} when polling in tight loops. */
    private long pinPageSourceLastProbeMs = 0L;
    private boolean pinPageSourceLastContainsPinStep = false;
    private long homePageSourceLastProbeMs = 0L;
    private boolean homePageSourceLastMarkersFound = false;
    private long versiBaruPageSourceLastProbeMs = 0L;
    private boolean versiBaruPageSourceLastHit = false;

    public MainWindowPage(AppiumDriver driver) {
        super(driver);
    }

    public void waitForMainWindowLoaded() {
        // Different apps expose different root elements to mac2.
        // Wait for common "app content" types instead of a specific window title.
        try {
            waitUtils().waitUntilVisible(BUTTONS);
            return;
        } catch (TimeoutException ignored) {
            // Fall back to static text.
        }
        waitUtils().waitUntilVisible(STATIC_TEXTS);
    }

    /** Widen the app window before interacting with embedded login fields (helps mac2 hit targets). */
    public void maximizeWindowBeforeCredentials() {
        maximizeMainWindow();
    }

    /** Close the app window after the post-login screenshot (delegates to BaseMacPage). */
    public void closeWindowAfterLogin() {
        closeMainWindow();
    }

    public void clickProfileAndLogout() {
        String[] profileMarkers = new String[] { "Profile", "Profil", "Akun", "Account" };
        String[] logoutMarkers = new String[] { "Logout", "Log out", "Sign out", "Keluar", "Sign Out" };

        boolean profileClicked = tryClickProfilePopUpButtonNative();
        if (!profileClicked) {
            for (String marker : profileMarkers) {
                if (tryClickElementContainingText(marker)) {
                    profileClicked = true;
                    break;
                }
            }
        }
        if (!profileClicked) {
            try {
                clickByXPath("//button[.//img[@alt='Profile']]");
                profileClicked = true;
            } catch (Exception ignored) {
            }
        }

        if (!profileClicked) {
            attachPinDebug("Profile click failed");
            throw new IllegalStateException(
                    "Could not open profile (XCUIElementTypePopUpButton / name Profile Profile, markers, or web avatar XPath)");
        }

        boolean logoutClicked = false;
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(8))
                    .pollingEvery(Duration.ofMillis(120))
                    .until(d -> {
                        if (tryClickLogoutNativeButton()) {
                            return true;
                        }
                        for (String marker : logoutMarkers) {
                            if (tryClickElementContainingText(marker)) {
                                return true;
                            }
                        }
                        for (WebElement el : macDriver().findElements(LOGOUT_WEB_BUTTON)) {
                            if (tryClick(el)) {
                                return true;
                            }
                        }
                        return false;
                    });
            logoutClicked = true;
        } catch (TimeoutException ignored) {
        }

        if (!logoutClicked) {
            try {
                clickByXPath(LOGOUT_BUTTON_XPATH);
                logoutClicked = true;
            } catch (Exception ignored) {
            }
        }

        if (!logoutClicked) {
            attachPinDebug("Logout click failed");
            throw new IllegalStateException("Could not click logout (XCUIElementTypeButton title Logout, markers, or web XPath)");
        }
    }

    /**
     * Profile entry per Accessibility Inspector: {@code XCUIElementTypePopUpButton}, title {@code Profile Profile}.
     */
    private boolean tryClickProfilePopUpButtonNative() {
        for (WebElement el : macDriver().findElements(POP_UP_BUTTONS)) {
            String t = safeElementLabel(el);
            if (t != null && t.toLowerCase().contains("profile")) {
                if (tryClick(el)) {
                    return true;
                }
            }
        }
        for (String name : new String[] { "Profile Profile", "Profile" }) {
            try {
                WebElement el = macDriver().findElement(By.name(name));
                if (tryClick(el)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * Logout per Accessibility Inspector: {@code XCUIElementTypeButton}, title {@code Logout}.
     */
    private boolean tryClickLogoutNativeButton() {
        for (WebElement el : macDriver().findElements(BUTTONS)) {
            String t = safeElementLabel(el);
            if (t != null && "logout".equalsIgnoreCase(t.trim())) {
                if (tryClick(el)) {
                    return true;
                }
            }
        }
        try {
            return tryClick(macDriver().findElement(By.name("Logout")));
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Waits until the app returns to the login screen after logout.
     * Relies on either:
     * - login labels like "Masuk"/"Login", or
     * - presence of both email text fields + password secure fields.
     */
    public void waitForLoginScreenVisible(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (isLoginScreenVisibleLikely()) {
                return;
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        attachPinDebug("Login screen did not become visible after logout");
        throw new IllegalStateException("Login screen not visible within " + timeout);
    }

    public boolean isLoginScreenVisibleLikely() {
        // If PIN is still on screen, we're not logged out yet.
        if (isPinStepVisibleBySource()) {
            return false;
        }
        boolean hasLoginMarkers = hasVisibleElementContaining("Masuk") || hasVisibleElementContaining("Login");
        boolean hasTextFields = !macDriver().findElements(TEXT_FIELDS).isEmpty();
        boolean hasSecureFields = !macDriver().findElements(SECURE_TEXT_FIELDS).isEmpty();
        return hasLoginMarkers || (hasTextFields && hasSecureFields);
    }

    /**
     * After launch, if the "Versi Baru Tersedia" dialog appears, clicks {@code Update Sekarang} and waits until
     * the login screen is usable again. If the dialog never appears within {@code probeTimeout}, returns immediately.
     * <p>
     * Uses the same mac2 strategy as the rest of this class: {@code XCUIElementTypeButton} / {@code StaticText} /
     * {@code Other} via {@link #tryClickElementContainingText} and {@link #hasVisibleElementContaining}, with an
     * XPath fallback for the web {@code <button>} markup.
     */
    public void handleOptionalVersiBaruAfterLaunch(Duration probeTimeout, Duration updateCompleteTimeout) {
        if (probeTimeout == null || probeTimeout.isNegative() || probeTimeout.isZero()) {
            throw new IllegalArgumentException("probeTimeout must be positive");
        }
        if (updateCompleteTimeout == null || updateCompleteTimeout.isNegative() || updateCompleteTimeout.isZero()) {
            throw new IllegalArgumentException("updateCompleteTimeout must be positive");
        }
        if (!pollUntilVersiBaruDialogVisible(probeTimeout)) {
            return;
        }

        boolean clicked = tryClickElementContainingText("Update Sekarang");
        if (!clicked) {
            try {
                clickByXPath(UPDATE_SEKARANG_XPATH);
                clicked = true;
            } catch (Exception e) {
                attachPinDebug("Update Sekarang click failed (Versi Baru dialog was visible)");
                throw new IllegalStateException("Could not click Update Sekarang on Versi Baru dialog", e);
            }
        }

        waitUntilLoginReadyAfterVersiBaruUpdate(updateCompleteTimeout);
    }

    private boolean pollUntilVersiBaruDialogVisible(Duration probeTimeout) {
        long deadlineNanos = System.nanoTime() + probeTimeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (isVersiBaruUpdateDialogVisible()) {
                return true;
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * Dialog detection: prefer accessibility tree (no {@code getPageSource}); throttle XML probes otherwise.
     */
    private boolean isVersiBaruUpdateDialogVisible() {
        if (hasVisibleElementContaining("Versi Baru Tersedia")) {
            return true;
        }
        if (hasVisibleElementContaining("Versi Baru")) {
            return true;
        }
        return versiBaruTersediaInPageSourceThrottled();
    }

    private boolean versiBaruTersediaInPageSourceUncached() {
        try {
            return String.valueOf(getDriver().getPageSource()).toLowerCase().contains("versi baru tersedia");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean versiBaruTersediaInPageSourceThrottled() {
        long now = System.currentTimeMillis();
        if (now - versiBaruPageSourceLastProbeMs < VERSI_BARU_PAGE_SOURCE_THROTTLE_MS) {
            return versiBaruPageSourceLastHit;
        }
        versiBaruPageSourceLastProbeMs = now;
        versiBaruPageSourceLastHit = versiBaruTersediaInPageSourceUncached();
        return versiBaruPageSourceLastHit;
    }

    /**
     * After an in-app update we expect the login surface — avoid {@link #isLoginScreenVisibleLikely()} here because it
     * calls {@link #isPinStepVisibleBySource()} (full page source) on every poll.
     */
    private void waitUntilLoginReadyAfterVersiBaruUpdate(Duration timeout) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (isLoginSurfaceReadyAfterVersiBaruUpdate()) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        attachPinDebug("Versi Baru update: login screen not ready within timeout");
        throw new IllegalStateException("App update did not return to a usable login screen within " + timeout);
    }

    private boolean isLoginSurfaceReadyAfterVersiBaruUpdate() {
        if (versiBaruTitleLikelyStillVisibleInTree()) {
            return false;
        }
        boolean hasLoginMarkers = hasVisibleElementContaining("Masuk") || hasVisibleElementContaining("Login");
        boolean hasTextFields = !macDriver().findElements(TEXT_FIELDS).isEmpty();
        boolean hasSecureFields = !macDriver().findElements(SECURE_TEXT_FIELDS).isEmpty();
        return hasLoginMarkers || (hasTextFields && hasSecureFields);
    }

    /** Element-only: still on the update modal when title is in the accessibility tree. */
    private boolean versiBaruTitleLikelyStillVisibleInTree() {
        return hasVisibleElementContaining("Versi Baru Tersedia") || hasVisibleElementContaining("Versi Baru");
    }

    /**
     * Clicks the login submit control using mac2's native button nodes.
     * This avoids flaky DOM/XPath matching for embedded webviews.
     */
    public void clickSubmitUsingButtons() {
        // Prefer the visible label (based on the app: "Masuk").
        if (tryClickElementContainingText("Masuk")) return;

        // Generic fallback: click first hittable XCUIElementTypeButton.
        if (tryClickFirstHittable(BUTTONS)) return;

        // Heuristic fallback: "Masuk" button is usually the lowest prominent button on the card.
        if (tryClickLowestVisibleButton(BUTTONS)) return;

        // Extra fallbacks for cases where the submit control isn't exposed as a button node.
        if (tryClickFirstHittable(OTHER_NODES)) return;
        if (tryClickFirstHittable(STATIC_TEXTS)) return;

        attachSubmitClickDebug();
        throw new IllegalStateException("Could not click submit using BUTTONS/other fallback nodes");
    }

    /**
     * Clicks the PIN step submit control. The web form uses a {@code type="button"} labeled "Submit".
     */
    public void clickPinSubmitUsingButtons() {
        if (tryClickElementContainingText("Submit")) {
            return;
        }
        if (tryClickLowestVisibleButton(BUTTONS)) {
            return;
        }
        if (tryClickFirstHittable(BUTTONS)) {
            return;
        }
        attachSubmitClickDebug();
        throw new IllegalStateException("Could not click PIN Submit using BUTTONS fallbacks");
    }

    /**
     * PIN UIs sometimes auto-submit after the last digit.
     * If "Submit" cannot be clicked but the home screen is already visible, treat it as success.
     */
    public void completePinStep() {
        if (postLoginHomeVisibleDuringPinFlow()) {
            return;
        }
        if (!isPinStepVisibleQuick()) {
            return;
        }

        try {
            clickPinSubmitUsingButtons();
        } catch (IllegalStateException submitFailure) {
            // Some builds auto-submit when the 4th digit is entered.
            try {
                new Actions(getDriver()).sendKeys(Keys.ENTER).perform();
            } catch (Exception ignored) {
            }
            if (waitUntilPinResolved(PIN_RESOLVE_TIMEOUT)) {
                return;
            }
            attachPinDebug("PIN submit click failed and home markers not visible");
            throw submitFailure;
        }
        if (!waitUntilPinResolved(PIN_RESOLVE_TIMEOUT)) {
            attachPinDebug("PIN submit was clicked but PIN modal still appears");
            throw new IllegalStateException("PIN submit attempted but flow did not resolve");
        }
    }

    /** Public probe used by test flow to short-circuit PIN when app is already logged in. */
    public boolean isLoggedInHomeVisible() {
        return isPostLoginHomeVisibleQuick() || isPostLoginStateLikely();
    }

    private boolean waitUntilPinResolved(Duration timeout) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (postLoginHomeVisibleDuringPinFlow()) {
                return true;
            }
            if (!isPinStepVisibleQuick()) {
                return true;
            }
            try {
                Thread.sleep(PIN_RESOLVE_POLL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return postLoginHomeVisibleDuringPinFlow() || !isPinStepVisibleQuick();
    }

    private boolean isPinStepVisibleBySource() {
        try {
            String src = String.valueOf(getDriver().getPageSource()).toLowerCase();
            return src.contains("pin-input-0") || src.contains("masukkan pin");
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Same as {@link #isPinStepVisibleBySource()} but avoids calling {@code getPageSource()} more than
     * once per {@link #PIN_PAGE_SOURCE_THROTTLE_MS} while polls run in a tight loop.
     */
    private boolean isPinStepVisibleBySourceThrottled() {
        long now = System.currentTimeMillis();
        if (now - pinPageSourceLastProbeMs < PIN_PAGE_SOURCE_THROTTLE_MS) {
            return pinPageSourceLastContainsPinStep;
        }
        pinPageSourceLastProbeMs = now;
        pinPageSourceLastContainsPinStep = isPinStepVisibleBySource();
        return pinPageSourceLastContainsPinStep;
    }

    /**
     * Clicks the visible button candidate with the largest Y coordinate (bottom-most) on the screen.
     * This avoids relying on label accessibility text which can be flaky in mac2/webviews.
     */
    private boolean tryClickLowestVisibleButton(By locator) {
        try {
            List<WebElement> elements = macDriver().findElements(locator);
            WebElement best = null;
            double bestCenterY = Double.NEGATIVE_INFINITY;
            for (WebElement el : elements) {
                try {
                    if (!el.isDisplayed()) continue;
                    var r = el.getRect();
                    double centerY = r.getY() + (r.getHeight() / 2.0);
                    if (centerY > bestCenterY) {
                        bestCenterY = centerY;
                        best = el;
                    }
                } catch (Exception ignored) {
                }
            }
            if (best == null) return false;
            return tryClick(best);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Interacts with the first available element among common interactive types.
     * This avoids hardcoding app-specific labels while we stabilize locators.
     */
    public void interactWithFirstAvailableElement() {
        if (tryDismissLoginBlockingOverlays()) {
            return;
        }
        if (tryClickFirstHittable(BUTTONS)) {
            return;
        }
        if (tryClickFirstHittable(OTHER_NODES)) {
            return;
        }
        if (tryClickFirstHittable(TEXT_FIELDS)) {
            return;
        }
        if (tryClickLowestVisibleButton(BUTTONS)) {
            return;
        }

        throw new IllegalStateException("Could not find any hittable button/text field to interact with");
    }

    /**
     * When the login form is blocked by the in-app update dialog, mac2 may not expose controls as
     * plain {@code XCUIElementTypeButton} with usable labels. Try label + XPath clicks first.
     */
    private boolean tryDismissLoginBlockingOverlays() {
        if (tryClickElementContainingText("Update Sekarang")) {
            return true;
        }
        if (tryClickAnyPresentByXPath(UPDATE_SEKARANG_XPATH)) {
            return true;
        }
        if (tryClickElementContainingText("Nanti Dulu")) {
            return true;
        }
        if (tryClickAnyPresentByXPath(NANTI_DULU_XPATH)) {
            return true;
        }
        return false;
    }

    private boolean tryClickAnyPresentByXPath(String xpath) {
        try {
            for (WebElement el : macDriver().findElements(By.xpath(xpath))) {
                if (tryClick(el)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Clicks an element using XPath (mac2 supports xpath strategy).
     * Useful when the UI is easier to target via web-like DOM locators.
     */
    public void clickByXPath(String xpath) {
        By locator = By.xpath(xpath);
        try {
            waitUtils().waitUntilClickable(locator).click();
            return;
        } catch (TimeoutException ignored) {
            // Fall back to a less strict wait (presence/visibility) because mac2
            // sometimes doesn't expose "enabled/clickable" reliably for DOM-like xpaths.
        }

        WaitUtils longerWait = new WaitUtils(getDriver(), Duration.ofSeconds(30));
        WebElement el = longerWait.waitUntilPresent(locator);
        el.click();
    }

    /**
     * Types into an element located by XPath.
     * Uses a visible wait, then click + clear + sendKeys; handy for webview inputs.
     */
    public void sendKeysByXPath(String xpath, CharSequence text) {
        WebElement el = macDriver().findElement(By.xpath(xpath));
        el.clear();
        el.click();
        //By el = locator;
        // try {
        //     el = waitUtils().waitUntilVisible(locator);
        // } catch (TimeoutException e) {
        //     // mac2 + styled web inputs (e.g. transparent PIN dots) are often present but not "visible".
        //     el = waitUtils().waitUntilPresent(locator);
        // }
        // try {
        //     el.click();
        // } catch (Exception ignored) {
        // }
        // try {
        //     el.clear();
        // } catch (Exception ignored) {
        // }
        el.sendKeys(text);
    }

    /**
     * Returns debug details for an element found by XPath.
     * Useful for logging what mac2 exposes for a specific locator.
     */
    public String getByXpath(String xpath) {
        try {
            By locator = By.xpath(xpath);
            WebElement el = new WaitUtils(getDriver(), Duration.ofSeconds(5)).waitUntilPresent(locator);
            String text = safeElementLabel(el);
            String rect = "n/a";
            try {
                var r = el.getRect();
                rect = "x=" + r.getX() + ",y=" + r.getY() + ",w=" + r.getWidth() + ",h=" + r.getHeight();
            } catch (Exception ignored) {
            }
            return "FOUND tag=" + el.getTagName() + ", label=" + (text == null ? "" : text) + ", rect={" + rect + "}";
        } catch (Exception e) {
            return "NOT_FOUND xpath=" + xpath + ", reason=" + e.getClass().getSimpleName();
        }
    }

    // Alias with conventional camel-case acronym spelling.
    public String getByXPath(String xpath) {
        return getByXpath(xpath);
    }

    /**
     * Submits the login form using the keyboard after {@link #enterCredentials(String, String)}.
     * Focus should still be on the password field; this sends {@code Tab} {@code tabsBeforeEnter} times
     * then {@code Enter} — avoids flaky button/ XPath clicks on mac2.
     * <p>
     * If submit does not fire, try {@code tabsBeforeEnter = 0} (Enter only) or {@code 2} depending on tab order.
     */
    public void submitLoginWithKeyboard(int tabsBeforeEnter) {
        if (tabsBeforeEnter < 0) {
            throw new IllegalArgumentException("tabsBeforeEnter must be >= 0");
        }
        Actions actions = new Actions(getDriver());
        // mac2/WDA doesn't accept Actions.pause() here (it expects a pointerMove first).
        // So we rely on sending keys sequentially.
        for (int i = 0; i < tabsBeforeEnter; i++) {
            actions.sendKeys(Keys.TAB);
        }
        actions.sendKeys(Keys.ENTER).perform();
    }

    /**
     * Same as {@link #submitLoginWithKeyboard(int)} with one {@code Tab} then {@code Enter}
     * (typical: password field → submit control).
     */
    public void submitLoginWithKeyboard() {
        // Default: after entering password, focus usually remains on password -> Enter submits.
        submitLoginWithKeyboard(0);
    }

    /**
     * Best-effort click for a "submit" button.
     * Mac2 XPath often doesn't map 1:1 with HTML DOM xpaths; if the xpath can't be found,
     * we fall back to clicking the first enabled button.
     */
    public void clickLoginSubmit(String submitXPath) {
        try {
            clickByXPath(submitXPath);
            return;
        } catch (TimeoutException | NoSuchElementException | InvalidSelectorException ignored) {
            // Mac2 may reject HTML-style XPath (XQuery parse) or the node may be missing — use fallbacks.
        }

        // Submit buttons can become visible/enabled shortly after entering credentials.
        // Wait a bit before giving up on fallbacks.
        WaitUtils longerWait = new WaitUtils(getDriver(), Duration.ofSeconds(30));
        try {
            longerWait.waitUntilVisible(BUTTONS);
        } catch (TimeoutException ignored) {
            // ignore and try other fallbacks
        }

        // Best-effort: click by visible/accessibility text (mac2 can misreport enabled/clickable).
        // Current app label is "Masuk" based on the login screen screenshot.
        if (tryClickElementContainingText("Masuk")) return;

        if (tryClickFirstHittable(BUTTONS)) return;
        if (tryClickFirstHittable(OTHER_NODES)) return;
        if (tryClickFirstHittable(STATIC_TEXTS)) return;

        attachSubmitClickDebug();
        throw new IllegalStateException("Could not click submit login button using xpath nor fallback button click");
    }

    private void attachSubmitClickDebug() {
        try {
            By[] locators = new By[] { BUTTONS, OTHER_NODES, STATIC_TEXTS };
            StringBuilder sb = new StringBuilder();
            sb.append("submit click debug (mac2)\n");
            for (By loc : locators) {
                List<WebElement> elements = macDriver().findElements(loc);
                sb.append("- ").append(loc).append(" count=").append(elements.size()).append("\n");
                int limit = Math.min(5, elements.size());
                for (int i = 0; i < limit; i++) {
                    WebElement el = elements.get(i);
                    String label = safeElementLabel(el);
                    boolean displayed = false;
                    try {
                        displayed = el.isDisplayed();
                    } catch (Exception ignored) {
                    }
                    String rectStr = "n/a";
                    try {
                        var r = el.getRect();
                        rectStr = "x=" + r.getX() + ",y=" + r.getY() + ",w=" + r.getWidth() + ",h=" + r.getHeight();
                    } catch (Exception ignored) {
                    }
                    sb.append("  #").append(i)
                            .append(" displayed=").append(displayed)
                            .append(" label=").append(label == null ? "null" : label)
                            .append(" rect={").append(rectStr).append("}\n");
                }
            }

            Allure.addAttachment("mac2 submit-click debug", "text/plain", sb.toString());
        } catch (Exception ignored) {
        }
    }

    private boolean tryClickFirstHittable(By locator) {
        List<WebElement> elements = macDriver().findElements(locator);
        if (elements.isEmpty()) {
            return false;
        }

        // mac2 sometimes finds nodes that are not marked `isDisplayed()` even though they are visually present.
        // Strategy:
        // 1) Try displayed nodes first (in order).
        // 2) If that doesn't work, try all remaining nodes (still in order).
        boolean triedAny = false;

        for (WebElement el : elements) {
            try {
                if (el.isDisplayed()) {
                    triedAny = true;
                    if (tryClick(el)) return true;
                }
            } catch (Exception ignored) {
                // ignore visibility issues, keep going
            }
        }

        // If nothing was "displayed" (or clicks failed), try every candidate anyway.
        for (WebElement el : elements) {
            if (!triedAny) {
                // We didn't consider any displayed nodes, so try from the top.
                if (tryClick(el)) return true;
            } else {
                // We already tried displayed nodes; now try remaining nodes too.
                if (tryClick(el)) return true;
            }
        }

        return false;
    }

    private boolean tryClick(WebElement el) {
        try {
            // Avoid long clickable waits on mac2 webview-backed elements; they often never become "clickable"
            // even when the visual control is ready.
            el.click();
            return true;
        } catch (Exception ignored) {
            return tryClickAtElementCenter(el);
        }
    }

    private boolean tryClickAtElementCenter(WebElement el) {
        try {
            // Coordinate-based click fallback: mac2 elementToBeClickable/element.click can be flaky
            // for embedded webviews; pointer actions can still hit the visual button.
            var rect = el.getRect();
            int centerX = (int) Math.round(rect.getX() + rect.getWidth() / 2.0);
            int centerY = (int) Math.round(rect.getY() + rect.getHeight() / 2.0);

            PointerInput mouse = new PointerInput(PointerInput.Kind.MOUSE, "mouse");
            Sequence click = new Sequence(mouse, 1);
            click.addAction(mouse.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), centerX, centerY));
            click.addAction(mouse.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            click.addAction(mouse.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

            getDriver().perform(List.of(click));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean tryClickElementContainingText(String partialText) {
        String needle = partialText == null ? "" : partialText.trim().toLowerCase();
        if (needle.isBlank()) {
            return false;
        }

        // Try multiple likely node types; the login button is often represented differently
        // depending on whether it's a native button or inside an embedded webview.
        By[] candidates = new By[] { BUTTONS, OTHER_NODES, STATIC_TEXTS };
        for (By locator : candidates) {
            List<WebElement> elements = macDriver().findElements(locator);
            for (WebElement el : elements) {
                String label = safeElementLabel(el);
                if (label != null && label.toLowerCase().contains(needle)) {
                    if (tryClick(el)) return true;
                }
            }
        }
        return false;
    }

    /**
     * Home detection without {@code getPageSource()}. Use while the PIN modal is up so we do not pull
     * multi‑MB XML on every poll (the SPA often keeps home strings in the DOM).
     */
    private boolean isPostLoginHomeVisibleFromElementsOnly() {
        By[] pools = new By[] { STATIC_TEXTS, BUTTONS, OTHER_NODES };
        for (By loc : pools) {
            List<WebElement> elements = macDriver().findElements(loc);
            for (WebElement el : elements) {
                String label = safeElementLabel(el);
                if (label == null) {
                    continue;
                }
                String labelLower = label.toLowerCase();
                for (String marker : HOME_UI_MARKERS) {
                    if (labelLower.contains(marker.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Throttled page-source scan for home markers (for {@link #waitForHomeDashboardVisible} and similar). */
    private boolean isPostLoginHomeMarkerInPageSourceThrottled() {
        long now = System.currentTimeMillis();
        if (now - homePageSourceLastProbeMs < HOME_PAGE_SOURCE_THROTTLE_MS) {
            return homePageSourceLastMarkersFound;
        }
        homePageSourceLastProbeMs = now;
        try {
            String src = String.valueOf(getDriver().getPageSource()).toLowerCase();
            for (String marker : HOME_UI_MARKERS) {
                if (src.contains(marker.toLowerCase())) {
                    homePageSourceLastMarkersFound = true;
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        homePageSourceLastMarkersFound = false;
        return false;
    }

    private boolean isPostLoginHomeVisibleQuick() {
        if (isPostLoginHomeVisibleFromElementsOnly()) {
            return true;
        }
        return isPostLoginHomeMarkerInPageSourceThrottled();
    }

    /**
     * During PIN/login-submit waits: never use page source for home detection (hidden web DOM makes source huge
     * and guarantees a full XML fetch almost every poll).
     */
    private boolean postLoginHomeVisibleDuringPinFlow() {
        return isPostLoginHomeVisibleFromElementsOnly();
    }

    /**
     * Fallback detection for "already progressed after login" when mac2 does not expose home labels.
     * If PIN source markers and login form controls are both absent, we treat it as post-login state.
     */
    private boolean isPostLoginStateLikely() {
        if (isPinStepVisibleBySource()) {
            return false;
        }
        boolean hasSecureField = !macDriver().findElements(SECURE_TEXT_FIELDS).isEmpty();
        boolean hasTextField = !macDriver().findElements(TEXT_FIELDS).isEmpty();
        boolean hasLoginLabel = hasVisibleElementContaining("Masuk") || hasVisibleElementContaining("Login");
        return !(hasSecureField || hasTextField || hasLoginLabel);
    }

    private String safeElementLabel(WebElement el) {
        // webview elements often expose text/label through different attributes depending on mac2 driver version.
        // Try a few common ones before giving up.
        String text = safeGetText(el);
        if (text != null) return text;

        String[] attrs = new String[] {
                "name",
                "title",
                "label",
                "value",
                "description",
                "identifier",
                "alt",
                "aria-label"
        };
        for (String attr : attrs) {
            String v = safeGetAttribute(el, attr);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private String safeGetText(WebElement el) {
        try {
            String t = el.getText();
            if (t != null) {
                t = t.trim();
                if (!t.isBlank()) return t;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String safeGetAttribute(WebElement el, String attr) {
        try {
            String v = el.getAttribute(attr);
            if (v != null) {
                v = v.trim();
                if (!v.isBlank()) return v;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Enters email + password into the first available input fields.
     * Assumes typical flow: first text field is email/username, next secure text field is password.
     */
    public void enterCredentials(String email, String password) {
        // Ensure the login form is present. Keep the element the wait found: mac2/WebView sometimes
        // returns an empty list from findElements(TEXT_FIELDS) right after visibilityOfElementLocated
        // succeeded, which would make firstUsableTextField(null) and fail spuriously.
        WebElement textFieldAnchor = waitUtils().waitUntilVisible(TEXT_FIELDS);

        List<WebElement> textFields = macDriver().findElements(TEXT_FIELDS);
        List<WebElement> secureFields = macDriver().findElements(SECURE_TEXT_FIELDS);

        // mac2 often reports isEnabled() = false for web-backed fields; prefer displayed, not enabled.
        WebElement emailField = firstUsableTextField(textFields);
        if (emailField == null) {
            emailField = textFieldAnchor;
        }

        emailField = ensureUsableForTyping(emailField);
        try {
            emailField.click();
        } catch (Exception ignored) {
            // click may fail if already focused
        }
        try {
            emailField.clear();
        } catch (Exception ignored) {
            // ignore if clear not supported
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                try {
                    emailField.click();
                } catch (Exception ignored) {
                }
                try {
                    emailField.clear();
                } catch (Exception ignored) {
                }
                emailField.sendKeys(email);
                break;
            } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                if (attempt == 2) {
                    throw stale;
                }
                List<WebElement> textFieldsRetry = macDriver().findElements(TEXT_FIELDS);
                WebElement emailFieldRetry = firstUsableTextField(textFieldsRetry);
                if (emailFieldRetry == null) {
                    throw stale;
                }
                emailField = ensureUsableForTyping(emailFieldRetry);
            }
        }

        WebElement passwordField = firstUsableTextField(secureFields);
        if (passwordField == null) {
            // Fallback: use second text field if secure fields aren't found.
            WebElement secondText = secondUsableTextField(textFields);
            if (secondText == null) {
                throw new IllegalStateException("No visible secure text field found for password (and no second text field fallback)");
            }
            passwordField = ensureUsableForTyping(secondText);
        } else {
            passwordField = ensureUsableForTyping(passwordField);
        }

        try {
            passwordField.click();
        } catch (Exception ignored) {
            // ignore
        }
        try {
            passwordField.clear();
        } catch (Exception ignored) {
            // ignore
        }
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                try {
                    passwordField.click();
                } catch (Exception ignored) {
                }
                try {
                    passwordField.clear();
                } catch (Exception ignored) {
                }
                passwordField.sendKeys(password);
                break;
            } catch (org.openqa.selenium.StaleElementReferenceException stale) {
                if (attempt == 2) {
                    throw stale;
                }
                List<WebElement> secureFieldsRetry = macDriver().findElements(SECURE_TEXT_FIELDS);
                WebElement passwordFieldRetry = firstUsableTextField(secureFieldsRetry);
                if (passwordFieldRetry == null) {
                    WebElement secondText = secondUsableTextField(macDriver().findElements(TEXT_FIELDS));
                    if (secondText == null) {
                        throw stale;
                    }
                    passwordFieldRetry = secondText;
                }
                passwordField = ensureUsableForTyping(passwordFieldRetry);
            }
        }

        // Re-focus password field for deterministic tab order before submitting.
        try {
            WebElement pf = firstUsableTextField(macDriver().findElements(SECURE_TEXT_FIELDS));
            if (pf != null) {
                pf = ensureUsableForTyping(pf);
                pf.click();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Enters a 4-digit PIN matching the web form: four {@code input} nodes with
     * {@code id="pin-input-0"} … {@code pin-input-3}, {@code maxlength="1"}, {@code inputmode="numeric"}.
     * <p>
     * Uses {@link WaitUtils#waitUntilPresent(By)} because mac2 often reports these fields as not visible
     * (e.g. {@code text-transparent}, small circular styling) while they are still interactable.
     */
    public void enterPinByInputs(String pin) {
        if (pin == null) {
            throw new IllegalArgumentException("pin must not be null");
        }
        String trimmed = pin.trim();
        if (trimmed.length() != 4) {
            throw new IllegalArgumentException("pin must be exactly 4 digits, got length=" + trimmed.length());
        }
        if (getPinTextFieldsByOrder().size() < 4
                && !isPinStepVisibleBySource()
                && !postLoginHomeVisibleDuringPinFlow()) {
            waitForPinTextFieldsPopulation(PIN_STEP_PROBE_TIMEOUT);
        }
        Exception primaryFailure = null;
        try {
            enterPinViaPerDigitLocators(trimmed);
            return;
        } catch (Exception e) {
            primaryFailure = e;
            attachPinDebug("PIN entry primary strategy failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // Fallback: many OTP/PIN UIs auto-advance with a single sendKeys sequence
        // once the first input is focused.
        if (tryEnterPinViaActiveElement(trimmed)) {
            return;
        }

        throw new IllegalStateException("Could not enter PIN with available mac2 strategies", primaryFailure);
    }

    /**
     * Waits for PIN step to appear before entering digits.
     * Helps separate "navigation never reached PIN screen" from "typing failed on PIN screen".
     */
    public void waitForPinStepVisible() {
        // Do not use isPostLoginStateLikely() here: after login submit, mac2 often briefly exposes
        // only the menu bar (no text fields / no Masuk), which falsely looks like "past login"
        // while the PIN webview is still loading.
        if (isPinStepVisibleBySource() || postLoginHomeVisibleDuringPinFlow()) {
            return;
        }
        long deadline = System.nanoTime() + PIN_STEP_PROBE_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            // Exit early if app already transitioned to home while we were waiting.
            if (postLoginHomeVisibleDuringPinFlow()) {
                return;
            }
            if (getPinTextFieldsByOrder().size() >= 4 || hasVisibleElementContaining("PIN")) {
                return;
            }
            try {
                Thread.sleep(PIN_STEP_POLL_INTERVAL.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        attachPinDebug("PIN step not visible");
        throw new IllegalStateException("PIN step did not appear (no pin-input-0/PIN title, and post-login state not detected)");
    }

    /**
     * After clicking login submit, wait for either:
     * (1) PIN step to become visible, or
     * (2) home screen to appear directly (auto-login/session restore).
     * <p>
     * Intentionally does not click submit again; login submit is handled by {@link #clickSubmitUsingButtons()}.
     */
    public void ensurePinStepAfterLoginSubmit() {
        if (isPinStepVisibleQuick() || postLoginHomeVisibleDuringPinFlow()) {
            return;
        }

        waitForPinStepVisible();
    }

    private boolean isPinStepVisibleQuick() {
        if (getPinTextFieldsByOrder().size() >= 4 || hasVisibleElementContaining("PIN")) {
            return true;
        }
        return isPinStepVisibleBySourceThrottled();
    }

    /**
     * mac2 can return a menu-only tree (no web fields) for a long beat after navigation; wait for
     * the embedded PIN inputs to appear as native text fields.
     */
    private void waitForPinTextFieldsPopulation(Duration timeout) {
        try {
            new WebDriverWait(getDriver(), timeout)
                    .pollingEvery(PIN_STEP_POLL_INTERVAL)
                    .until(d -> getPinTextFieldsByOrder().size() >= 4
                            || isPinStepVisibleQuick()
                            || postLoginHomeVisibleDuringPinFlow());
        } catch (TimeoutException ignored) {
            // proceed; enterPinViaPerDigitLocators / fallbacks will fail with debug attachment
        }
    }

    /**
     * Waits until post-login home/dashboard markers are visible (Market, Watchlist, Portfolio, etc.).
     *
     * @throws IllegalStateException if the timeout elapses first
     */
    public void waitForHomeDashboardVisible(Duration timeout) {
        try {
            new WebDriverWait(getDriver(), timeout)
                    .pollingEvery(HOME_DASHBOARD_POLL_INTERVAL)
                    .until(d -> isPostLoginHomeVisibleQuick());
        } catch (TimeoutException e) {
            attachPinDebug("Home/dashboard not visible within " + timeout.toSeconds() + "s");
            throw new IllegalStateException("Home or dashboard did not become visible within " + timeout, e);
        }
    }

    private void enterPinViaPerDigitLocators(String pin) {
        List<WebElement> pinFields = getPinTextFieldsByOrder();
        if (pinFields.size() < 4) {
            throw new IllegalStateException("Expected at least 4 XCUIElementTypeTextField elements for PIN, found " + pinFields.size());
        }

        for (int i = 0; i < 4; i++) {
            String digit = String.valueOf(pin.charAt(i));
            WebElement field = pinFields.get(i);
            try {
                field.click();
            } catch (Exception ignored) {
            }
            try {
                field.clear();
            } catch (Exception ignored) {
            }
            field.sendKeys(digit);
        }
    }

    private boolean tryEnterPinViaActiveElement(String pin) {
        try {
            List<WebElement> pinFields = getPinTextFieldsByOrder();
            if (!pinFields.isEmpty()) {
                try {
                    pinFields.get(0).click();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
            // best-effort only
        }

        try {
            getDriver().switchTo().activeElement().sendKeys(pin);
            return true;
        } catch (Exception ignored) {
        }
        try {
            new Actions(getDriver()).sendKeys(pin).perform();
            return true;
        } catch (Exception ignored) {
            attachPinDebug("PIN active-element fallback failed");
            return false;
        }
    }

    private List<WebElement> getPinTextFieldsByOrder() {
        List<WebElement> fields = macDriver().findElements(TEXT_FIELDS);
        return fields == null ? java.util.Collections.emptyList() : fields;
    }

    private boolean hasVisibleElementContaining(String token) {
        String needle = token == null ? "" : token.trim().toLowerCase();
        if (needle.isBlank()) {
            return false;
        }
        By[] pools = new By[] { STATIC_TEXTS, BUTTONS, OTHER_NODES };
        for (By pool : pools) {
            for (WebElement el : macDriver().findElements(pool)) {
                String label = safeElementLabel(el);
                if (label != null && label.toLowerCase().contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void attachPinDebug(String title) {
        try {
            String source = getDriver().getPageSource();
            Allure.addAttachment(title, "text/xml", source);
        } catch (Exception ignored) {
        }
    }

    /**
     * Picks a field mac2 can type into: prefers displayed+enabled, then displayed, else first in list.
     */
    private WebElement firstUsableTextField(List<WebElement> elements) {
        for (WebElement el : elements) {
            try {
                if (el.isDisplayed() && el.isEnabled()) {
                    return el;
                }
            } catch (Exception ignored) {
            }
        }
        for (WebElement el : elements) {
            try {
                if (el.isDisplayed()) {
                    return el;
                }
            } catch (Exception ignored) {
            }
        }
        if (!elements.isEmpty()) {
            return elements.get(0);
        }
        return null;
    }

    private WebElement secondUsableTextField(List<WebElement> elements) {
        WebElement first = null;
        for (WebElement el : elements) {
            try {
                if (!el.isDisplayed()) {
                    continue;
                }
                if (first == null) {
                    first = el;
                } else {
                    return el;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Waits for visibility; avoids strict {@code elementToBeClickable} which fails when mac2 misreports enabled.
     */
    private WebElement ensureUsableForTyping(WebElement el) {
        try {
            return waitUtils().waitUntilVisible(el);
        } catch (Exception e) {
            try {
                return waitUtils().waitUntilClickable(el);
            } catch (Exception e2) {
                return el;
            }
        }
    }
}

