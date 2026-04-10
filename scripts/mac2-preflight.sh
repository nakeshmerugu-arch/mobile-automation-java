#!/usr/bin/env bash

set -u

PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0

pass() {
  printf "PASS: %s\n" "$1"
  PASS_COUNT=$((PASS_COUNT + 1))
}

warn() {
  printf "WARN: %s\n" "$1"
  WARN_COUNT=$((WARN_COUNT + 1))
}

fail() {
  printf "FAIL: %s\n" "$1"
  FAIL_COUNT=$((FAIL_COUNT + 1))
}

section() {
  printf "\n== %s ==\n" "$1"
}

section "Platform"
if [[ "$(uname -s)" == "Darwin" ]]; then
  pass "Running on macOS"
else
  fail "Not running on macOS. Mac2 requires macOS host."
fi

section "Xcode"
if command -v xcode-select >/dev/null 2>&1; then
  DEV_DIR="$(xcode-select -p 2>/dev/null || true)"
  if [[ -n "${DEV_DIR}" ]]; then
    pass "xcode-select active developer directory: ${DEV_DIR}"
  else
    fail "xcode-select is present but no active developer directory."
  fi
else
  fail "xcode-select not found."
fi

if command -v xcodebuild >/dev/null 2>&1; then
  XCODE_VER="$(xcodebuild -version 2>/dev/null | tr '\n' '; ' || true)"
  if [[ -n "${XCODE_VER}" ]]; then
    pass "Xcode detected: ${XCODE_VER}"
  else
    warn "xcodebuild exists but version could not be read."
  fi
else
  warn "xcodebuild not found in PATH."
fi

section "automationmodetool"
if [[ -x "/usr/bin/automationmodetool" ]]; then
  pass "/usr/bin/automationmodetool exists and is executable"
  AM_STATUS="$(/usr/bin/automationmodetool 2>/dev/null || true)"
  if [[ -n "${AM_STATUS}" ]]; then
    pass "automationmodetool status: ${AM_STATUS//$'\n'/ | }"
  else
    warn "Could not read automation mode status (may require elevated privileges)."
  fi
else
  fail "/usr/bin/automationmodetool not found. Run: sudo xcode-select -s /Applications/Xcode.app/Contents/Developer"
fi

section "Node / Appium / mac2"
if command -v node >/dev/null 2>&1; then
  pass "Node: $(node -v)"
else
  fail "Node.js not found."
fi

if command -v npm >/dev/null 2>&1; then
  pass "npm: $(npm -v)"
else
  fail "npm not found."
fi

if command -v appium >/dev/null 2>&1; then
  pass "Appium CLI found: $(appium --version 2>/dev/null || echo "version unavailable")"
  MAC2_LIST="$(appium driver list --installed 2>/dev/null || true)"
  if [[ "${MAC2_LIST}" == *"mac2"* ]]; then
    pass "Appium mac2 driver appears installed"
  else
    fail "Appium mac2 driver is not installed. Run: appium driver install mac2"
  fi
else
  fail "Appium CLI not found. Install with: npm install -g appium@latest"
fi

section "Appium server port"
if command -v lsof >/dev/null 2>&1; then
  if lsof -nP -iTCP:4723 -sTCP:LISTEN >/dev/null 2>&1; then
    pass "Appium server appears to be listening on 4723"
  else
    warn "No listener on TCP 4723. Start with: appium server --address 127.0.0.1 --port 4723"
  fi
else
  warn "lsof not found; cannot verify Appium server port."
fi

section "Summary"
printf "PASS: %d | WARN: %d | FAIL: %d\n" "${PASS_COUNT}" "${WARN_COUNT}" "${FAIL_COUNT}"

if [[ "${FAIL_COUNT}" -gt 0 ]]; then
  exit 1
fi

exit 0
