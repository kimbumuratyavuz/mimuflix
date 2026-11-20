package com.example.downloader

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.DownloadedEpisode
import com.example.model.DownloadedMovie
import com.example.model.DownloadedSeriesFolder
import com.example.parser.M3uParser
import java.io.File

object StorageHelper {

    private const val FOLDER_NAME = "Mimu_Downloader"

    fun getDownloadDirectory(context: Context): File {
        return try {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val mimuDir = File(publicDir, FOLDER_NAME)
            if (!mimuDir.exists()) {
                mimuDir.mkdirs()
            }
            if (mimuDir.canWrite()) {
                mimuDir
            } else {
                val appDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FOLDER_NAME)
                if (!appDir.exists()) appDir.mkdirs()
                appDir
            }
        } catch (_: Exception) {
            val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FOLDER_NAME)
            if (!fallback.exists()) fallback.mkdirs()
            fallback
        }
    }

    fun getMoviesDirectory(context: Context): File {
        val base = getDownloadDirectory(context)
        val dir = File(base, "Filmler")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSeriesDirectory(context: Context): File {
        val base = getDownloadDirectory(context)
        val dir = File(base, "Diziler")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSeriesFolder(context: Context, seriesName: String): File {
        val base = getSeriesDirectory(context)
        val safeName = M3uParser.sanitizeFileName(seriesName).ifEmpty { "Dizi" }
        val dir = File(base, safeName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createOutputFile(
        context: Context,
        rawTitle: String,
        isTs: Boolean = false,
        isSeries: Boolean = false,
        seriesName: String? = null
    ): File {
        val dir = if (isSeries) {
            getSeriesFolder(context, seriesName ?: "Dizi")
        } else {
            getMoviesDirectory(context)
        }
        val ext = if (isTs) ".ts" else ".mp4"
        val baseName = M3uParser.sanitizeFileName(rawTitle)
        var file = File(dir, "$baseName$ext")
        var counter = 1
        while (file.exists() && file.length() > 0) {
            file = File(dir, "${baseName}_$counter$ext")
            counter++
        }
        return file
    }

    fun scanMediaFile(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("video/mp4", "video/mp2t", "video/*")
            ) { _, _ -> }
        } catch (_: Exception) {
            // Scanner failed, file is still saved on disk
        }
    }

    fun cleanEmptySeriesFolders(context: Context) {
        try {
            val seriesDir = getSeriesDirectory(context)
            seriesDir.listFiles()?.forEach { folder ->
                if (folder.isDirectory) {
                    val files = folder.listFiles()
                    if (files == null || files.isEmpty()) {
                        folder.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun deleteFileAndCleanEmptyFolder(context: Context, file: File) {
        try {
            val parent = file.parentFile
            if (file.exists()) {
                file.delete()
            }
            if (parent != null && parent.isDirectory && parent != getMoviesDirectory(context) && parent != getSeriesDirectory(context) && parent != getDownloadDirectory(context)) {
                val remaining = parent.listFiles()
                if (remaining == null || remaining.isEmpty()) {
                    parent.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun openInExternalPlayer(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "Dosya bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Videoyu Oynat").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Oynatıcı açılamadı: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun openDownloadsFolder(context: Context) {
        val dir = getDownloadDirectory(context)
        try {
            val downloadIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(downloadIntent)
            return
        } catch (_: Exception) {}

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                dir
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "İndirilenleri Aç").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "İndirilenler klasörü: ${dir.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
