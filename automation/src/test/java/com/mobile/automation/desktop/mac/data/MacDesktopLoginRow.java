package com.mobile.automation.desktop.mac.data;

/**
 * One row of mac desktop login test data (JSON-mapped). Keep payloads out of test methods.
 */
public record MacDesktopLoginRow(String id, String email, String password, String pin) {}
