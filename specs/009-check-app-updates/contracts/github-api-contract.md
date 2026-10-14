# Contract: GitHub Releases REST API

**Feature Branch**: `009-check-app-updates`
**Created**: 2026-10-14
**Status**: Completed

## 1. Endpoint Specification

- **Target URL**: `https://api.github.com/repos/letroll/droidplane/releases/latest`
- **HTTP Method**: `GET`
- **Request Headers**:
  - `Accept`: `application/vnd.github.v3+json`
  - `User-Agent`: `Droidplane-Android-App`

---

## 2. Expected Response Schema (JSON)

```json
{
  "tag_name": "v1.0.25",
  "name": "Release v1.0.25",
  "published_at": "2026-10-14T10:00:00Z",
  "html_url": "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
  "body": "Detailed release notes and bug fixes...",
  "draft": false,
  "prerelease": false,
  "assets": [
    {
      "name": "app-release.apk",
      "content_type": "application/vnd.android.package-archive",
      "size": 18456123,
      "browser_download_url": "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk"
    }
  ]
}
```

### Extraction Mapping
- **Tag Name**: `root.getString("tag_name")`
- **Release Name**: `root.optString("name", tagName)`
- **Published Date**: `root.optString("published_at")`
- **Release Body**: `root.optString("body")`
- **Webpage URL**: `root.getString("html_url")`
- **APK Asset**: Iterate over `root.getJSONArray("assets")` to find the first asset with `name.endsWith(".apk")`:
  - `browser_download_url` -> `apkDownloadUrl`
  - `name` -> `apkFileName`
  - `size` -> `apkSize`

---

## 3. Error Codes & Handling
- **200 OK**: Normal response containing latest release.
- **404 Not Found**: No releases found on repository -> Result treated as `UpToDate`.
- **403 Forbidden (Rate Limit)**: GitHub rate limit exceeded -> Handled with graceful error message.
- **Network / Socket Timeout**: Timeout set to 5000ms -> Gracefully handled as `UpdateCheckResult.Error`.
