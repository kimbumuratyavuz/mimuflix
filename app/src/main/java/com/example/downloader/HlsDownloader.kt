package com.example.downloader

import android.content.Context
import com.example.model.DownloadTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class HlsDownloader(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    companion object {
        private const val DIRECT_PARALLEL_CHANNELS = 3
        private const val HLS_PARALLEL_WORKERS = 3
        private const val BUFFER_SIZE = 64 * 1024
        private const val MIN_SIZE_FOR_MULTI_CHANNEL = 6 * 1024 * 1024L // 6 MB
    }

    suspend fun download(
        task: DownloadTask,
        onProgress: (downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long, elapsedMs: Long) -> Unit,
        isPaused: () -> Boolean,
        isCancelled: () -> Boolean
    ): File = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis() - task.elapsedTimeMs

        val isHls = task.streamUrl.contains(".m3u8", ignoreCase = true)
        val outputFile = if (task.filePath != null) {
            File(task.filePath)
        } else {
            StorageHelper.createOutputFile(
                context = context,
                rawTitle = task.title,
                isTs = isHls,
                isSeries = task.isSeries,
                seriesName = task.seriesName
            )
        }

        if (!isHls) {
            downloadDirectFile(
                task,
                outputFile,
                startTime,
                onProgress,
                isPaused,
                isCancelled
            )
        } else {
            downloadHlsStream(
                task,
                outputFile,
                startTime,
                onProgress,
                isPaused,
                isCancelled
            )
        }

        StorageHelper.scanMediaFile(context, outputFile)
        outputFile
    }

    private suspend fun downloadDirectFile(
        task: DownloadTask,
        outputFile: File,
        startTime: Long,
        onProgress: (Long, Long, Long, Long) -> Unit,
        isPaused: () -> Boolean,
        isCancelled: () -> Boolean
    ) {
        val baseRequestBuilder = Request.Builder().url(task.streamUrl)
        task.httpUserAgent?.let { baseRequestBuilder.header("User-Agent", it) }
        task.httpReferrer?.let { baseRequestBuilder.header("Referer", it) }

        // Probe for Content-Length and Accept-Ranges support
        val probe = probeServer(baseRequestBuilder.build())
        val supportsRanges = probe.supportsRanges
        val contentLength = probe.contentLength

        val existingLength = if (outputFile.exists()) outputFile.length() else 0L

        if (supportsRanges && contentLength >= MIN_SIZE_FOR_MULTI_CHANNEL) {
            // Multi-channel segmented range download
            downloadMultiChannel(
                task = task,
                baseRequestBuilder = baseRequestBuilder,
                outputFile = outputFile,
                totalBytes = contentLength,
                startTime = startTime,
                onProgress = onProgress,
                isPaused = isPaused,
                isCancelled = isCancelled
            )
        } else {
            // Single channel streaming download with resume & pause-safe reconnect
            downloadSingleChannel(
                baseRequestBuilder = baseRequestBuilder,
                outputFile = outputFile,
                initialDownloaded = existingLength,
                contentLength = contentLength,
                startTime = startTime,
                onProgress = onProgress,
                isPaused = isPaused,
                isCancelled = isCancelled
            )
        }
    }

    private data class ServerProbe(val contentLength: Long, val supportsRanges: Boolean)

    private fun probeServer(request: Request): ServerProbe {
        return try {
            val headRequest = request.newBuilder().head().build()
            client.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val lengthHeader = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    val acceptRanges = response.header("Accept-Ranges")
                    val supports = acceptRanges.equals("bytes", ignoreCase = true) || lengthHeader > 0
                    ServerProbe(lengthHeader, supports)
                } else {
                    ServerProbe(-1L, false)
                }
            }
        } catch (_: Exception) {
            ServerProbe(-1L, false)
        }
    }

    private suspend fun downloadMultiChannel(
        task: DownloadTask,
        baseRequestBuilder: Request.Builder,
        outputFile: File,
        totalBytes: Long,
        startTime: Long,
        onProgress: (Long, Long, Long, Long) -> Unit,
        isPaused: () -> Boolean,
        isCancelled: () -> Boolean
    ) {
        val raf = RandomAccessFile(outputFile, "rw")
        try {
            if (raf.length() < totalBytes) {
                raf.setLength(totalBytes)
            }
        } catch (_: Exception) { }

        val fileWriteLock = Any()
        val numChannels = DIRECT_PARALLEL_CHANNELS
        val partSize = totalBytes / numChannels

        val channelProgress = Array(numChannels) { AtomicLong(0L) }
        val channelStarts = LongArray(numChannels) { i -> i * partSize }
        val channelEnds = LongArray(numChannels) { i ->
            if (i == numChannels - 1) totalBytes - 1 else ((i + 1) * partSize) - 1
        }

        // Initialize from existing file if resuming
        if (task.downloadedBytes > 0 && outputFile.exists()) {
            val approxPerChannel = (task.downloadedBytes / numChannels).coerceAtLeast(0L)
            for (i in 0 until numChannels) {
                val maxForChannel = channelEnds[i] - channelStarts[i] + 1
                channelProgress[i].set(approxPerChannel.coerceAtMost(maxForChannel))
            }
        }

        val totalDownloadedAtomic = AtomicLong(channelProgress.sumOf { it.get() })
        val lastProgressUpdate = AtomicLong(System.currentTimeMillis())
        val lastBytesCount = AtomicLong(totalDownloadedAtomic.get())

        fun checkAndNotifyProgress(force: Boolean = false) {
            val now = System.currentTimeMillis()
            val diff = now - lastProgressUpdate.get()
            if (force || diff >= 450) {
                val current = totalDownloadedAtomic.get()
                val prevBytes = lastBytesCount.getAndSet(current)
                lastProgressUpdate.set(now)

                val speed = if (diff > 0) ((current - prevBytes) * 1000) / diff else 0L
                val elapsed = now - startTime
                onProgress(current.coerceAtMost(totalBytes), totalBytes, speed.coerceAtLeast(0L), elapsed)
            }
        }

        while (totalDownloadedAtomic.get() < totalBytes && !isCancelled()) {
            while (isPaused()) {
                delay(300)
                if (isCancelled()) throw Exception("İndirme iptal edildi")
            }

            // Launch channels in parallel
            coroutineScope {
                val jobs = (0 until numChannels).map { channelIndex ->
                    async(Dispatchers.IO) {
                        val cStart = channelStarts[channelIndex]
                        val cEnd = channelEnds[channelIndex]
                        val cProgress = channelProgress[channelIndex]

                        var currentOffset = cStart + cProgress.get()
                        if (currentOffset > cEnd) return@async

                        while (currentOffset <= cEnd && !isCancelled() && !isPaused()) {
                            val chunkReq = baseRequestBuilder.build().newBuilder()
                                .header("Range", "bytes=$currentOffset-$cEnd")
                                .build()

                            try {
                                client.newCall(chunkReq).execute().use { response ->
                                    if (!response.isSuccessful && response.code != 206) {
                                        throw Exception("Kanal $channelIndex HTTP ${response.code}")
                                    }
                                    val body = response.body ?: return@use
                                    val stream = body.byteStream()
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var read: Int

                                    while (stream.read(buffer).also { read = it } != -1) {
                                        if (isCancelled() || isPaused()) break

                                        synchronized(fileWriteLock) {
                                            raf.seek(currentOffset)
                                            raf.write(buffer, 0, read)
                                        }

                                        currentOffset += read
                                        cProgress.addAndGet(read.toLong())
                                        totalDownloadedAtomic.addAndGet(read.toLong())
                                        checkAndNotifyProgress(false)
                                    }
                                }
                            } catch (e: Exception) {
                                if (isCancelled()) throw e
                                if (!isPaused()) {
                                    delay(500)
                                }
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }

            if (isPaused()) {
                checkAndNotifyProgress(force = true)
                while (isPaused()) {
                    delay(300)
                    if (isCancelled()) throw Exception("İndirme iptal edildi")
                }
            }
        }

        try {
            raf.close()
        } catch (_: Exception) {}

        if (isCancelled()) throw Exception("İndirme iptal edildi")
        checkAndNotifyProgress(force = true)
    }

    private suspend fun downloadSingleChannel(
        baseRequestBuilder: Request.Builder,
        outputFile: File,
        initialDownloaded: Long,
        contentLength: Long,
        startTime: Long,
        onProgress: (Long, Long, Long, Long) -> Unit,
        isPaused: () -> Boolean,
        isCancelled: () -> Boolean
    ) {
        var downloaded = initialDownloaded
        val totalBytes = if (contentLength > 0) {
            if (downloaded > 0) downloaded + contentLength else contentLength
        } else 0L

        var lastTime = System.currentTimeMillis()
        var lastBytes = downloaded
        var isDone = false

        while (!isDone && !isCancelled()) {
            while (isPaused()) {
                delay(300)
                if (isCancelled()) throw Exception("İndirme iptal edildi")
            }

            val request = if (downloaded > 0 && outputFile.exists()) {
                baseRequestBuilder.build().newBuilder().header("Range", "bytes=$downloaded-").build()
            } else {
                baseRequestBuilder.build()
            }

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw Exception("HTTP ${response.code}: ${response.message}")
                    }
                    val body = response.body ?: throw Exception("Boş yanıt alındı")
                    val bodyLength = body.contentLength()
                    val resolvedTotal = if (totalBytes > 0) totalBytes else if (bodyLength > 0) downloaded + bodyLength else 0L

                    val buffer = ByteArray(BUFFER_SIZE)
                    body.byteStream().use { input ->
                        FileOutputStream(outputFile, downloaded > 0 && outputFile.exists()).use { output ->
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                if (isCancelled()) throw Exception("İndirme iptal edildi")
                                if (isPaused()) {
                                    // Cleanly exit current stream and wait loop, avoids 30s SocketTimeout!
                                    output.flush()
                                    break
                                }

                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead

                                val now = System.currentTimeMillis()
                                val diff = now - lastTime
                                if (diff >= 500) {
                                    val byteDiff = downloaded - lastBytes
                                    val currentSpeed = (byteDiff * 1000) / diff
                                    lastTime = now
                                    lastBytes = downloaded
                                    val elapsed = now - startTime
                                    onProgress(downloaded, resolvedTotal.coerceAtLeast(downloaded), currentSpeed, elapsed)
                                }
                            }
                            output.flush()
                            if (!isPaused() && bytesRead == -1) {
                                isDone = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (isCancelled()) throw e
                if (!isPaused()) {
                    delay(1000)
                }
            }
        }

        if (isCancelled()) throw Exception("İndirme iptal edildi")
        val finalElapsed = System.currentTimeMillis() - startTime
        onProgress(downloaded, if (totalBytes > 0) totalBytes else downloaded, 0L, finalElapsed)
    }

    private suspend fun downloadHlsStream(
        task: DownloadTask,
        outputFile: File,
        startTime: Long,
        onProgress: (Long, Long, Long, Long) -> Unit,
        isPaused: () -> Boolean,
        isCancelled: () -> Boolean
    ) {
        val playlistText = fetchString(task.streamUrl, task.httpUserAgent, task.httpReferrer)
        var mediaPlaylistUrl = task.streamUrl
        var mediaPlaylistText = playlistText

        if (playlistText.contains("#EXT-X-STREAM-INF", ignoreCase = true)) {
            val variant = extractBestVariantUrl(task.streamUrl, playlistText)
            if (variant != null) {
                mediaPlaylistUrl = variant
                mediaPlaylistText = fetchString(mediaPlaylistUrl, task.httpUserAgent, task.httpReferrer)
            }
        }

        val segments = extractSegmentUrls(mediaPlaylistUrl, mediaPlaylistText)
        if (segments.isEmpty()) {
            throw Exception("M3U8 akışında video segmenti bulunamadı.")
        }

        val totalSegments = segments.size
        var downloadedBytes = outputFile.length()
        val estimatedTotal = if (task.totalBytes > 0) task.totalBytes else (totalSegments * 1_250_000L)

        // Calculate resume point if resuming
        val initialSkipSegments = if (downloadedBytes > 1_500_000L) {
            ((downloadedBytes / 1_250_000L).toInt() - 1).coerceIn(0, totalSegments - 1)
        } else 0

        val nextIndexToDownload = AtomicInteger(initialSkipSegments)
        val nextIndexToWrite = AtomicInteger(initialSkipSegments)
        val downloadedSegments = ConcurrentHashMap<Int, ByteArray>()

        var lastTime = System.currentTimeMillis()
        var lastBytes = downloadedBytes
        var currentSpeed = 0L

        FileOutputStream(outputFile, true).use { output ->
            while (nextIndexToWrite.get() < totalSegments && !isCancelled()) {
                while (isPaused()) {
                    delay(300)
                    if (isCancelled()) throw Exception("İndirme iptal edildi")
                }

                // Multi-worker concurrent segment fetching
                coroutineScope {
                    val activeWorkers = (0 until HLS_PARALLEL_WORKERS).map {
                        async(Dispatchers.IO) {
                            while (!isCancelled() && !isPaused()) {
                                // Limit buffer size to prevent RAM overflow
                                if (downloadedSegments.size >= HLS_PARALLEL_WORKERS * 2) {
                                    delay(80)
                                    continue
                                }

                                val segIndex = nextIndexToDownload.getAndIncrement()
                                if (segIndex >= totalSegments) break

                                val segUrl = segments[segIndex]
                                val segRequest = Request.Builder().url(segUrl)
                                task.httpUserAgent?.let { segRequest.header("User-Agent", it) }
                                task.httpReferrer?.let { segRequest.header("Referer", it) }

                                try {
                                    client.newCall(segRequest.build()).execute().use { resp ->
                                        if (resp.isSuccessful) {
                                            val bytes = resp.body?.bytes()
                                            if (bytes != null) {
                                                downloadedSegments[segIndex] = bytes
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    if (isCancelled()) throw e
                                    // If failed, roll back index so it can be retried
                                    nextIndexToDownload.decrementAndGet()
                                    delay(400)
                                }
                            }
                        }
                    }

                    // Writer logic in current coroutine: write segments in strict order
                    while (nextIndexToWrite.get() < totalSegments && !isCancelled() && !isPaused()) {
                        val writeIdx = nextIndexToWrite.get()
                        val chunk = downloadedSegments.remove(writeIdx)
                        if (chunk != null) {
                            output.write(chunk)
                            downloadedBytes += chunk.size
                            nextIndexToWrite.incrementAndGet()

                            val now = System.currentTimeMillis()
                            val diff = now - lastTime
                            if (diff >= 450) {
                                val byteDiff = downloadedBytes - lastBytes
                                currentSpeed = (byteDiff * 1000) / diff
                                lastTime = now
                                lastBytes = downloadedBytes

                                val dynamicTotal = estimatedTotal.coerceAtLeast(downloadedBytes)
                                val elapsed = now - startTime
                                onProgress(downloadedBytes, dynamicTotal, currentSpeed, elapsed)
                            }
                        } else {
                            delay(40)
                        }
                    }

                    activeWorkers.awaitAll()
                }

                output.flush()
            }
        }

        if (isCancelled()) throw Exception("İndirme iptal edildi")
        val finalElapsed = System.currentTimeMillis() - startTime
        onProgress(downloadedBytes, downloadedBytes, 0L, finalElapsed)
    }

    private fun fetchString(url: String, userAgent: String?, referrer: String?): String {
        val req = Request.Builder().url(url)
        userAgent?.let { req.header("User-Agent", it) }
        referrer?.let { req.header("Referer", it) }

        client.newCall(req.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}: ${resp.message}")
            return resp.body?.string() ?: ""
        }
    }

    private fun extractBestVariantUrl(masterUrl: String, playlist: String): String? {
        val lines = playlist.lines()
        var nextIsVariant = false
        var bestVariant: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("#EXT-X-STREAM-INF", ignoreCase = true)) {
                nextIsVariant = true
                continue
            }
            if (nextIsVariant && !line.startsWith("#") && line.isNotEmpty()) {
                bestVariant = resolveUrl(masterUrl, line)
                break
            }
        }
        return bestVariant
    }

    private fun extractSegmentUrls(mediaPlaylistUrl: String, playlist: String): List<String> {
        val list = mutableListOf<String>()
        val lines = playlist.lines()
        var isNextSegment = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                isNextSegment = true
                continue
            }
            if (isNextSegment && !line.startsWith("#") && line.isNotEmpty()) {
                list.add(resolveUrl(mediaPlaylistUrl, line))
                isNextSegment = false
            }
        }
        return list
    }

    private fun resolveUrl(baseUrl: String, pathOrUrl: String): String {
        if (pathOrUrl.startsWith("http://", ignoreCase = true) || pathOrUrl.startsWith("https://", ignoreCase = true)) {
            return pathOrUrl
        }
        val resolved = try {
            val uri = URI(baseUrl)
            uri.resolve(pathOrUrl).toString()
        } catch (_: Exception) {
            val baseWithoutQuery = baseUrl.substringBefore('?')
            val baseDir = if (baseWithoutQuery.contains('/')) baseWithoutQuery.substringBeforeLast('/') + "/" else baseWithoutQuery + "/"
            baseDir + pathOrUrl
        }

        return if (!resolved.contains('?') && baseUrl.contains('?')) {
            val query = baseUrl.substringAfter('?')
            "$resolved?$query"
        } else {
            resolved
        }
    }
}
