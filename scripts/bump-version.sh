#!/usr/bin/env bash
set -euo pipefail

GRADLE_FILE="app/build.gradle.kts"

if [ ! -f "$GRADLE_FILE" ]; then
    echo "Error: $GRADLE_FILE not found" >&2
    exit 1
fi

CURRENT_CODE=$(awk -F'=' '/val appVersionCode/{gsub(/[^0-9]/, "", $2); print $2}' "$GRADLE_FILE")

if [ -z "$CURRENT_CODE" ]; then
    echo "Error: Could not extract appVersionCode from $GRADLE_FILE" >&2
    exit 1
fi

NEW_CODE=$((CURRENT_CODE + 1))
NEW_NAME="1.0.${NEW_CODE}"

echo "Bumping version: $CURRENT_CODE -> $NEW_CODE (versionName: $NEW_NAME)"

# Portable replacement for both macOS (BSD) and Linux (GNU)
if sed --version 2>/dev/null | grep -q GNU; then
    sed -i -E 's/(val appVersionCode[[:space:]]*=[[:space:]]*)[0-9]+/\1'"$NEW_CODE"'/' "$GRADLE_FILE"
    sed -i -E 's/(val appVersionName[[:space:]]*=[[:space:]]*")[^"]+(")/\1'"$NEW_NAME"'\2/' "$GRADLE_FILE"
else
    sed -i.bak -E 's/(val appVersionCode[[:space:]]*=[[:space:]]*)[0-9]+/\1'"$NEW_CODE"'/' "$GRADLE_FILE"
    sed -i.bak -E 's/(val appVersionName[[:space:]]*=[[:space:]]*")[^"]+(")/\1'"$NEW_NAME"'\2/' "$GRADLE_FILE"
    rm -f "${GRADLE_FILE}.bak"
fi

echo "NEW_CODE=$NEW_CODE"
echo "NEW_NAME=$NEW_NAME"
