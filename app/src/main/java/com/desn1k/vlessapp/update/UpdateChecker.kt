package com.desn1k.vlessapp.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the GitHub "latest release" API to find a newer build of this app.
 * No auth token is used (public repo, unauthenticated rate limit is enough for this).
 */
object UpdateChecker {

    private const val OWNER = "desn1k"
    private const val REPO = "vlessapp"

    suspend fun fetchLatestRelease(): GitHubRelease? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != 200) return@withContext null

            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            GitHubRelease(
                tagName = json.optString("tag_name"),
                htmlUrl = json.optString("html_url"),
                apkDownloadUrl = apkUrl,
                body = json.optString("body")
            )
        } catch (t: Throwable) {
            null
        }
    }

    /** Compares "vX.Y.Z"-style tags / plain version names. Falls back to plain inequality. */
    fun isNewer(remoteTag: String, currentVersionName: String): Boolean {
        val remote = normalize(remoteTag)
        val current = normalize(currentVersionName)
        if (remote == current) return false

        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        if (remoteParts.isEmpty() || currentParts.isEmpty()) return remote != current

        for (i in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    private fun normalize(version: String) = version.trim().removePrefix("v").removePrefix("V")
}
