#!/usr/bin/env bash
# Run all on-device E2E tests (com.anplak.androidmusic.ui.*E2ETest) on a Wi-Fi paired Android device.
#
# Usage:
#   ./scripts/run-e2e-wifi.sh                    # auto-pick first "device" from adb
#   ./scripts/run-e2e-wifi.sh 192.168.1.42:5555  # explicit serial (adb connect first)
#   ANDROID_SERIAL=192.168.1.42:5555 ./scripts/run-e2e-wifi.sh
#
# Pair over Wi-Fi (one-time):
#   adb pair <ip>:<pairing-port>
#   adb connect <ip>:<debug-port>
#   adb devices

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Android Gradle Plugin requires JDK 17–21 (Java 26+ breaks the build).
if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -qE '"1[789]\.|"2[01]\.'; then
  for candidate in \
    "/Users/aplakhotnyi/Library/Java/JavaVirtualMachines/corretto-21.0.6/Contents/Home" \
    "/Users/aplakhotnyi/Library/Java/JavaVirtualMachines/corretto-23.0.2/Contents/Home"; do
    if [[ -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME=$JAVA_HOME"
  "$JAVA_HOME/bin/java" -version 2>&1 | head -1
fi

DEVICE="${1:-${ANDROID_SERIAL:-}}"

if [[ -z "$DEVICE" ]]; then
  DEVICE="$(adb devices | awk 'NR>1 && $2=="device" { print $1; exit }')"
fi

if [[ -z "$DEVICE" ]]; then
  echo "No adb device found. Pair/connect over Wi-Fi, then retry." >&2
  echo "  adb devices" >&2
  exit 1
fi

export ANDROID_SERIAL="$DEVICE"
echo "Using device: $ANDROID_SERIAL"

adb -s "$ANDROID_SERIAL" wait-for-device
adb -s "$ANDROID_SERIAL" shell input keyevent KEYCODE_WAKEUP || true

E2E_PACKAGE="com.anplak.androidmusic.ui"
echo "Running E2E tests in $E2E_PACKAGE ..."

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package="$E2E_PACKAGE"

echo "E2E finished on $ANDROID_SERIAL"
