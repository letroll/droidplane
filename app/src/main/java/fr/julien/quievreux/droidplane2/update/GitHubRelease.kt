package fr.julien.quievreux.droidplane2.update

import org.json.JSONObject

/**
 * Domain entity representing a release retrieved from GitHub.
 */
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val publishedAt: String,
    val body: String,
    val htmlUrl: String,
    val apkDownloadUrl: String? = null,
    val apkFileName: String? = null,
    val apkSize: Long? = null
) {
    companion object {
        fun fromJsonObject(json: JSONObject): GitHubRelease {
            val tagName = json.getString("tag_name")
            val name = json.optString("name").ifBlank { tagName }
            val publishedAt = json.optString("published_at", "")
            val body = json.optString("body", "")
            val htmlUrl = json.getString("html_url")

            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long? = null

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.getString("browser_download_url")
                        apkName = assetName
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            return GitHubRelease(
                tagName = tagName,
                name = name,
                publishedAt = publishedAt,
                body = body,
                htmlUrl = htmlUrl,
                apkDownloadUrl = apkUrl,
                apkFileName = apkName,
                apkSize = apkSize
            )
        }

        fun fromJsonString(jsonStr: String): GitHubRelease =
            fromJsonObject(JSONObject(jsonStr))
    }
}
