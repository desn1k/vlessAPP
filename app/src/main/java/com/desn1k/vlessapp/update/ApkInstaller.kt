package com.desn1k.vlessapp.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads an APK asset from a GitHub release and launches the system package installer.
 * Requires the user to allow "install unknown apps" for this app (REQUEST_INSTALL_PACKAGES).
 */
object ApkInstaller {

    sealed class DownloadProgress {
        data class Progress(val percent: Int) : DownloadProgress()
        data class Done(val file: File) : DownloadProgress()
        data class Failed(val message: String) : DownloadProgress()
    }

    /** Names the cached file after the release tag so switching versions doesn't reuse a stale cache. */
    private fun cacheFileFor(context: Context, url: String): File =
        File(context.cacheDir, "vless-checker-update-${url.hashCode()}.apk")

    suspend fun download(context: Context, url: String, onProgress: (DownloadProgress) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val outFile = cacheFileFor(context, url)

                val headConnection = URL(url).openConnection() as HttpURLConnection
                headConnection.instanceFollowRedirects = true
                headConnection.requestMethod = "HEAD"
                headConnection.connectTimeout = 10000
                headConnection.readTimeout = 15000
                headConnection.connect()
                val expectedLength = headConnection.contentLength
                headConnection.disconnect()

                if (expectedLength > 0 && outFile.exists() && outFile.length() == expectedLength.toLong()) {
                    onProgress(DownloadProgress.Done(outFile))
                    return@withContext
                }

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.connect()

                val total = connection.contentLength
                connection.inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress(DownloadProgress.Progress((downloaded * 100L / total).toInt()))
                            }
                        }
                    }
                }
                onProgress(DownloadProgress.Done(outFile))
            } catch (t: Throwable) {
                onProgress(DownloadProgress.Failed(t.message ?: "Download failed"))
            }
        }
    }

    fun install(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
