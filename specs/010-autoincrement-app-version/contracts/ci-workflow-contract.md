# CI Workflow Contract: Version Auto-Increment

**Feature Branch**: `010-autoincrement-app-version`
**Created**: 2026-10-14
**Status**: Completed

## 1. Local Bump Script (`scripts/bump-version.sh`)

```bash
#!/usr/bin/env bash
set -euo pipefail

GRADLE_FILE="app/build.gradle.kts"

# Extract current version code
CURRENT_CODE=$(grep -E 'val appVersionCode\s*=' "$GRADLE_FILE" | sed -E 's/.*val appVersionCode\s*=\s*([0-9]+).*/\1/')

if [ -z "$CURRENT_CODE" ]; then
    echo "Error: Could not extract appVersionCode from $GRADLE_FILE" >&2
    exit 1
fi

NEW_CODE=$((CURRENT_CODE + 1))
NEW_NAME="1.0.${NEW_CODE}"

echo "Bumping version: $CURRENT_CODE -> $NEW_CODE (versionName: $NEW_NAME)"

# Replace in app/build.gradle.kts
sed -i.bak -E "s/(val appVersionCode\s*=\s*)[0-9]+/\1${NEW_CODE}/" "$GRADLE_FILE"
sed -i.bak -E "s/(val appVersionName\s*=\s*\")[^\"]+(\")/\1${NEW_NAME}\2/" "$GRADLE_FILE"
rm -f "${GRADLE_FILE}.bak"

echo "NEW_CODE=$NEW_CODE"
echo "NEW_NAME=$NEW_NAME"
```

---

## 2. GitHub Actions Integration (`.github/workflows/android_release.yml`)

```yaml
      - name: 🔢 Auto-incrémenter la version
        run: |
          chmod +x scripts/bump-version.sh
          ./scripts/bump-version.sh
          
          NEW_CODE=$(grep -E 'val appVersionCode\s*=' app/build.gradle.kts | sed -E 's/.*val appVersionCode\s*=\s*([0-9]+).*/\1/')
          NEW_NAME="1.0.${NEW_CODE}"
          
          echo "APP_VERSION_NAME=${NEW_NAME}" >> $GITHUB_ENV
          echo "APP_VERSION_CODE=${NEW_CODE}" >> $GITHUB_ENV
          
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add app/build.gradle.kts
          git commit -m "chore(release): bump version to ${NEW_NAME} [skip ci]"
          git push origin master
```

Release step:
```yaml
      - name: 🚀 Publier sur GitHub Releases
        uses: softprops/action-gh-release@v2
        with:
          tag_name: v${{ env.APP_VERSION_NAME }}
          name: "Release v${{ env.APP_VERSION_NAME }}"
          body: "APK de release signé généré automatiquement suite au push sur master."
          draft: false
          prerelease: false
          files: app/build/outputs/apk/release/*.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```
