package com.mobile.automation.desktop.mac.pages;

import com.mobile.automation.desktop.mac.BaseMacPage;
import com.mobile.automation.utils.WaitUtils;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
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
    /** Min interval between home-marker scans in page source (PIN flow must not call this every poll). */
    private static final long HOME_PAGE_SOURCE_THROTTLE_MS = 300L;
    /** Slow fallback probe interval for PIN markers in page source. */
    private static final long PIN_PAGE_SOURCE_THROTTLE_MS = 3000L;
    /** Min interval for "Versi Baru" text probes in page source during dialog polling. */
    private static final long VERSI_BARU_PAGE_SOURCE_THROTTLE_MS = 300L;
    /** Post-login home hints exposed in the accessibility tree (no {@code getPageSource}). */
    private static final String[] HOME_UI_MARKERS =
            new String[] { "Market", "Watchlist", "Portfolio", "Trending", "Movers", "Papan Khusus" };
    /** Native profile control: Accessibility Inspector shows type Pop Up Button, title {@code Profile Profile}. */
    private static final By POP_UP_BUTTONS = By.className("XCUIElementTypePopUpButton");
    /** Poll while waiting for home/dashboard markers after PIN. */
    private static final Duration HOME_DASHBOARD_POLL_INTERVAL = Duration.ofMillis(200);
    /** Cap expensive XCUI list scans so one probe can't take minutes on huge trees. */
    private static final int MAX_SCAN_ELEMENTS_PER_POOL = 40;
    private static final int MAX_CLICK_CANDIDATES_PER_POOL = 25;
    // Generic locators for mac2/XCUIElementType trees.
    // Using generic types makes the test robust until we capture real accessibility identifiers.
    private static final By STATIC_TEXTS = By.className("XCUIElementTypeStaticText");
    private static final By BUTTONS = By.className("XCUIElementTypeButton");
    // Some apps (or embedded web UIs) render buttons as "Other" or other non-button nodes.
    private static final By OTHER_NODES = By.className("XCUIElementTypeOther");
    private static final By TEXT_FIELDS = By.className("XCUIElementTypeTextField");
    private static final By SECURE_TEXT_FIELDS = By.className("XCUIElementTypeSecureTextField");
    private static final String[] HOME_FAST_NAMES =
            new String[] { "Market", "Watchlist", "Portfolio", "Trending", "Movers", "Papan Khusus" };
    private static final String[] PIN_FAST_NAMES =
            new String[] { "Masukkan PIN", "PIN", "Buat PIN", "Enter PIN", "OTP" };

    private long homePageSourceLastProbeMs = 0L;
    private boolean homePageSourceLastMarkersFound = false;
    private long pinPageSourceLastProbeMs = 0L;
    private boolean pinPageSourceLastHit = false;
    private long versiBaruPageSourceLastProbeMs = 0L;
    private boolean versiBaruPageSourceLastHit = false;

    public MainWindowPage(AppiumDriver driver) {
        super(driver);
    }

    public void waitForMainWindowLoaded() {
        // Fast exact-name probes first; avoids broad pool waits on large XCUI trees.
        if (hasAnyElementByExactName("Masuk", "Login", "Profile", "Portfolio")) {
            return;
        }
        try {
            waitUtils().waitUntilVisible(BUTTONS);
            return;
        } catch (TimeoutException ignored) {
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
        openProfileMenu();
        clickLogoutAction();
    }

    public void openProfileMenu() {
        String[] profileNames = new String[] { "Profile Profile", "Profile", "Profil", "Akun", "Account" };
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < deadline) {
            if (tryClickProfilePopUpButtonNative()) {
                return;
            }
            if (tryClickExactNameInPools(profileNames, POP_UP_BUTTONS, BUTTONS, OTHER_NODES)) {
                return;
            }
            if (tryClickElementContainingTextInPools("Profile", POP_UP_BUTTONS, BUTTONS, OTHER_NODES)) {
                return;
            }
            try {
                Thread.sleep(120);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        attachPinDebug("Profile click failed");
        throw new IllegalStateException("Could not open profile menu");
    }

    public void clickLogoutAction() {
        String[] logoutNames = new String[] { "Logout", "Log out", "Sign out", "Keluar", "Sign Out" };
        long deadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
        while (System.nanoTime() < deadline) {
            if (tryClickLogoutNativeButton()) {
                return;
            }
            if (tryClickExactNameInPools(logoutNames, BUTTONS, OTHER_NODES)) {
                return;
            }
            if (tryClickElementContainingTextInPools("Logout", BUTTONS, OTHER_NODES)) {
                return;
            }
            try {
                Thread.sleep(120);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        attachPinDebug("Logout click failed");
        throw new IllegalStateException("Could not click logout");
    }

    /**
     * Profile entry per Accessibility Inspector: {@code XCUIElementTypePopUpButton}, title {@code Profile Profile}.
     */
    private boolean tryClickProfilePopUpButtonNative() {
        List<WebElement> popups = macDriver().findElements(POP_UP_BUTTONS);
        int limit = Math.min(MAX_CLICK_CANDIDATES_PER_POOL, popups.size());
        for (int i = 0; i < limit; i++) {
            WebElement el = popups.get(i);
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
        List<WebElement> buttons = macDriver().findElements(BUTTONS);
        int limit = Math.min(MAX_CLICK_CANDIDATES_PER_POOL, buttons.size());
        for (int i = 0; i < limit; i++) {
            WebElement el = buttons.get(i);
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
     * {@code Other} via {@link #tryClickElementContainingText} and {@link #hasVisibleElementContaining}.
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
            attachPinDebug("Update Sekarang click failed (Versi Baru dialog was visible)");
            throw new IllegalStateException("Could not click Update Sekarang on Versi Baru dialog");
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
        if (tryClickElementContainingTextInPools("Masuk", BUTTONS, OTHER_NODES)) return;

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
        if (tryClickElementContainingTextInPools("Submit", BUTTONS, OTHER_NODES)) {
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

    /** True when PIN input fields are currently exposed in the XCUI tree. */
    public boolean hasPinInputsVisible() {
        if (getPinTextFieldsByOrder().size() >= 4) {
            return true;
        }
        // Some builds expose only the title/OTP shell first; accept that as PIN visible.
        if (hasAnyElementByExactName(PIN_FAST_NAMES)) {
            return true;
        }
        return pinMarkerInPageSourceThrottled();
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
     * Clicks the visible button candidate with the largest Y coordinate (bottom-most) on the screen.
     * This avoids relying on label accessibility text which can be flaky in mac2/webviews.
     */
    private boolean tryClickLowestVisibleButton(By locator) {
        try {
            List<WebElement> elements = macDriver().findElements(locator);
            int limit = Math.min(MAX_CLICK_CANDIDATES_PER_POOL, elements.size());
            WebElement best = null;
            double bestCenterY = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < limit; i++) {
                WebElement el = elements.get(i);
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
     * plain {@code XCUIElementTypeButton} with usable labels. Try text-based XCUI clicks first.
     */
    private boolean tryDismissLoginBlockingOverlays() {
        if (tryClickElementContainingText("Update Sekarang")) {
            return true;
        }
        if (tryClickElementContainingText("Nanti Dulu")) {
            return true;
        }
        return false;
    }

    /**
     * Submits the login form using the keyboard after {@link #enterCredentials(String, String)}.
     * Focus should still be on the password field; this sends {@code Tab} {@code tabsBeforeEnter} times
     * then {@code Enter} — avoids flaky point-and-click behavior on mac2.
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
     * Best-effort click for a login submit control using XCUI element pools only.
     * The {@code submitXPath} parameter is intentionally ignored to prevent XPath-based desktop actions.
     */
    public void clickLoginSubmit(@SuppressWarnings("unused") String submitXPath) {
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
        if (tryClickElementContainingTextInPools("Masuk", BUTTONS, OTHER_NODES)) return;

        if (tryClickFirstHittable(BUTTONS)) return;
        if (tryClickFirstHittable(OTHER_NODES)) return;
        if (tryClickFirstHittable(STATIC_TEXTS)) return;

        attachSubmitClickDebug();
        throw new IllegalStateException("Could not click submit login button using XCUI fallback pools");
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
        int limit = Math.min(MAX_CLICK_CANDIDATES_PER_POOL, elements.size());

        // mac2 sometimes finds nodes that are not marked `isDisplayed()` even though they are visually present.
        // Strategy:
        // 1) Try displayed nodes first (in order).
        // 2) If that doesn't work, try all remaining nodes (still in order).
        boolean triedAny = false;

        for (int i = 0; i < limit; i++) {
            WebElement el = elements.get(i);
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
        for (int i = 0; i < limit; i++) {
            WebElement el = elements.get(i);
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
        try {
            WebElement exact = macDriver().findElement(By.name(partialText));
            if (tryClick(exact)) {
                return true;
            }
        } catch (Exception ignored) {
        }

        // Try multiple likely node types; the login button is often represented differently
        // depending on whether it's a native button or inside an embedded webview.
        By[] candidates = new By[] { BUTTONS, OTHER_NODES, STATIC_TEXTS };
        for (By locator : candidates) {
            List<WebElement> elements = macDriver().findElements(locator);
            int limit = Math.min(MAX_SCAN_ELEMENTS_PER_POOL, elements.size());
            for (int i = 0; i < limit; i++) {
                WebElement el = elements.get(i);
                String label = safeElementLabel(el);
                if (label != null && label.toLowerCase().contains(needle)) {
                    if (tryClick(el)) return true;
                }
            }
        }
        return false;
    }

    private boolean tryClickElementContainingTextInPools(String partialText, By... pools) {
        String needle = partialText == null ? "" : partialText.trim().toLowerCase();
        if (needle.isBlank()) {
            return false;
        }
        // First try exact name in the provided pools to avoid matching heading/static labels.
        for (By pool : pools) {
            List<WebElement> elements = macDriver().findElements(pool);
            int limit = Math.min(MAX_SCAN_ELEMENTS_PER_POOL, elements.size());
            for (int i = 0; i < limit; i++) {
                WebElement el = elements.get(i);
                String label = safeElementLabel(el);
                if (label != null && label.trim().equalsIgnoreCase(partialText)) {
                    if (tryClick(el)) return true;
                }
            }
        }
        // Then allow partial contains within the same pools.
        for (By pool : pools) {
            List<WebElement> elements = macDriver().findElements(pool);
            int limit = Math.min(MAX_SCAN_ELEMENTS_PER_POOL, elements.size());
            for (int i = 0; i < limit; i++) {
                WebElement el = elements.get(i);
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
        if (hasAnyElementByExactName(HOME_FAST_NAMES)) {
            return true;
        }
        By[] pools = new By[] { STATIC_TEXTS, BUTTONS, OTHER_NODES };
        for (By loc : pools) {
            List<WebElement> elements = macDriver().findElements(loc);
            int limit = Math.min(MAX_SCAN_ELEMENTS_PER_POOL, elements.size());
            for (int i = 0; i < limit; i++) {
                WebElement el = elements.get(i);
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
        if (hasPinInputsVisible()) {
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
        if (getPinTextFieldsByOrder().size() < 4 && !postLoginHomeVisibleDuringPinFlow()) {
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
        // Keep this probe lightweight: avoid page-source scans and broad text-marker traversals.
        if (getPinTextFieldsByOrder().size() >= 4 || postLoginHomeVisibleDuringPinFlow()) {
            return;
        }
        long deadline = System.nanoTime() + PIN_STEP_PROBE_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            // Exit early if app already transitioned to home while we were waiting.
            if (postLoginHomeVisibleDuringPinFlow()) {
                return;
            }
            if (getPinTextFieldsByOrder().size() >= 4) {
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
        ensurePinStepAfterLoginSubmit(PIN_STEP_PROBE_TIMEOUT);
    }

    /**
     * Bounded wait after login submit for either PIN inputs or home transition.
     * Uses XCUI element checks only (no page-source probing).
     */
    public void ensurePinStepAfterLoginSubmit(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (hasPinInputsVisible() || postLoginHomeVisibleDuringPinFlow() || isPostLoginStateLikely()) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        attachPinDebug("PIN step not visible within " + timeout.toSeconds() + "s after login submit");
        throw new IllegalStateException("PIN step (or home) did not appear within " + timeout + " after login submit");
    }

    private boolean isPinStepVisibleQuick() {
        return hasPinInputsVisible();
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
        if (fields == null || fields.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        // Prefer true PIN fields when identifiers are exposed (pin-input-0..3), then fall back to first visible set.
        java.util.List<WebElement> pinLike = new java.util.ArrayList<>();
        int limit = Math.min(Math.max(MAX_SCAN_ELEMENTS_PER_POOL, 80), fields.size());
        for (int i = 0; i < limit; i++) {
            WebElement el = fields.get(i);
            String label = safeElementLabel(el);
            if (label != null && label.toLowerCase().contains("pin-input-")) {
                pinLike.add(el);
            }
        }
        if (!pinLike.isEmpty()) {
            return pinLike;
        }
        return fields;
    }

    private boolean pinMarkerInPageSourceThrottled() {
        long now = System.currentTimeMillis();
        if (now - pinPageSourceLastProbeMs < PIN_PAGE_SOURCE_THROTTLE_MS) {
            return pinPageSourceLastHit;
        }
        pinPageSourceLastProbeMs = now;
        try {
            String src = String.valueOf(getDriver().getPageSource()).toLowerCase();
            pinPageSourceLastHit = src.contains("pin-input-0")
                    || src.contains("masukkan pin")
                    || src.contains("pin")
                    || src.contains("otp");
        } catch (Exception ignored) {
            pinPageSourceLastHit = false;
        }
        return pinPageSourceLastHit;
    }

    private boolean hasVisibleElementContaining(String token) {
        String needle = token == null ? "" : token.trim().toLowerCase();
        if (needle.isBlank()) {
            return false;
        }
        if (hasAnyElementByExactName(token)) {
            return true;
        }
        By[] pools = new By[] { STATIC_TEXTS, BUTTONS, OTHER_NODES };
        for (By pool : pools) {
            List<WebElement> elements = macDriver().findElements(pool);
            int limit = Math.min(MAX_SCAN_ELEMENTS_PER_POOL, elements.size());
            for (int i = 0; i < limit; i++) {
                WebElement el = elements.get(i);
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

    private boolean hasAnyElementByExactName(String... names) {
        if (names == null || names.length == 0) {
            return false;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                List<WebElement> elements = macDriver().findElements(By.name(name));
                if (!elements.isEmpty()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean tryClickExactNameInPools(String[] names, By... pools) {
        if (names == null || names.length == 0) {
            return false;
        }
        for (By pool : pools) {
            List<WebElement> elements = macDriver().findElements(pool);
            int limit = Math.min(MAX_SCAN_ELEMENTS_PER_POOL, elements.size());
            for (int i = 0; i < limit; i++) {
                WebElement el = elements.get(i);
                String label = safeElementLabel(el);
                if (label == null) {
                    continue;
                }
                for (String name : names) {
                    if (name != null && !name.isBlank() && label.trim().equalsIgnoreCase(name)) {
                        if (tryClick(el)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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

