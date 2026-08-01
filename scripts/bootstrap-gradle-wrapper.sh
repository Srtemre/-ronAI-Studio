#!/usr/bin/env bash
# Bootstraps the Gradle wrapper for the HTML to APK Studio project.
# Run once after extracting the source archive.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f gradle/wrapper/gradle-wrapper.jar ]; then
    echo "Gradle wrapper already present."
    exit 0
fi

if ! command -v gradle >/dev/null 2>&1; then
    echo "ERROR: system 'gradle' not found."
    echo "Install Gradle 8.5+ (https://gradle.org/install/) and re-run this script,"
    echo "OR open the project in Android Studio which will install the wrapper for you."
    exit 1
fi

echo "Generating Gradle wrapper using system gradle..."
gradle wrapper --gradle-version 8.9 --distribution-type bin
chmod +x gradlew

echo "Done. You can now run ./gradlew assembleDebug"
