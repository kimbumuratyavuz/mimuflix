package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.MimuApplication
import com.example.R
import com.example.downloader.HlsDownloader
import com.example.model.DownloadStatus
import com.example.model.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class DownloadService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val isPausedFlag = AtomicBoolean(false)
    private val isCancelledFlag = AtomicBoolean(false)

    companion object {
        const val ACTION_START_QUEUE = "com.example.service.START_QUEUE"
        const val ACTION_PAUSE = "com.example.service.PAUSE"
        const val ACTION_RESUME = "com.example.service.RESUME"
        const val ACTION_CANCEL = "com.example.service.CANCEL"

        private const val NOTIFICATION_ID = 2001

        private val _currentActiveTask = MutableStateFlow<DownloadTask?>(null)
        val currentActiveTask: StateFlow<DownloadTask?> = _currentActiveTask.asStateFlow()

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _isServicePaused = MutableStateFlow(false)
        val isServicePaused: StateFlow<Boolean> = _isServicePaused.asStateFlow()

        fun startQueue(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_QUEUE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MimuDownloader::DownloadWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_QUEUE -> {
                isCancelledFlag.set(false)
                isPausedFlag.set(false)
                _isServicePaused.value = false
                _isServiceActive.value = true
                acquireWakeLock()
                startForegroundWithNotification("İndirme kuyruğu başlatılıyor…", 0, "0 KB/s")
                startProcessingQueue()
            }
            ACTION_PAUSE -> {
                isPausedFlag.set(true)
                _isServicePaused.value = true
                _currentActiveTask.value?.let { current ->
                    val paused = current.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L)
                    _currentActiveTask.value = paused
                    updateNotification(current.title, calculatePercent(current.downloadedBytes, current.totalBytes), "Duraklatıldı", isPaused = true)
                    serviceScope.launch {
                        MimuApplication.database.downloadTaskDao().update(paused)
                    }
                }
            }
            ACTION_RESUME -> {
                isCancelledFlag.set(false)
                isPausedFlag.set(false)
                _isServicePaused.value = false
                _isServiceActive.value = true
                acquireWakeLock()
                _currentActiveTask.value?.let { current ->
                    val resumed = current.copy(status = DownloadStatus.DOWNLOADING)
                    _currentActiveTask.value = resumed
                    updateNotification(current.title, calculatePercent(current.downloadedBytes, current.totalBytes), "Devam ediliyor…", isPaused = false)
                    serviceScope.launch {
                        MimuApplication.database.downloadTaskDao().update(resumed)
                    }
                }
                if (downloadJob == null || downloadJob?.isActive == false) {
                    startProcessingQueue()
                }
            }
            ACTION_CANCEL -> {
                isCancelledFlag.set(true)
                isPausedFlag.set(false)
                _isServicePaused.value = false
                downloadJob?.cancel()
                stopForegroundTask()
            }
        }
        return START_NOT_STICKY
    }

    private fun startProcessingQueue() {
        if (downloadJob?.isActive == true) return

        downloadJob = serviceScope.launch {
            val dao = MimuApplication.database.downloadTaskDao()

            while (!isCancelledFlag.get()) {
                val actionableTasks = dao.getTasksByStatuses(
                    listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.PENDING)
                )
                if (actionableTasks.isEmpty()) {
                    break
                }

                val task = actionableTasks.first()
                val isHls = task.streamUrl.contains(".m3u8", ignoreCase = true)
                val targetFile = if (task.filePath != null) {
                    java.io.File(task.filePath)
                } else {
                    com.example.downloader.StorageHelper.createOutputFile(
                        context = applicationContext,
                        rawTitle = task.title,
                        isTs = isHls,
                        isSeries = task.isSeries,
                        seriesName = task.seriesName
                    )
                }

                val runningTask = task.copy(
                    status = DownloadStatus.DOWNLOADING,
                    filePath = targetFile.absolutePath
                )
                dao.update(runningTask)
                _currentActiveTask.value = runningTask

                updateNotification(
                    runningTask.title,
                    calculatePercent(runningTask.downloadedBytes, runningTask.totalBytes),
                    "İndiriliyor…",
                    isPaused = false
                )

                var lastDbUpdateTime = 0L

                try {
                    val downloader = HlsDownloader(applicationContext)
                    val completedFile = downloader.download(
                        task = runningTask,
                        onProgress = { downloaded, total, speed, elapsed ->
                            val updated = runningTask.copy(
                                downloadedBytes = downloaded,
                                totalBytes = total,
                                speedBytesPerSec = speed,
                                elapsedTimeMs = elapsed,
                                status = if (isPausedFlag.get()) DownloadStatus.PAUSED else DownloadStatus.DOWNLOADING
                            )
                            _currentActiveTask.value = updated
                            val percent = calculatePercent(downloaded, total)
                            val speedStr = formatSpeed(speed)
                            val text = "$speedStr • ${formatSize(downloaded)} / ${formatSize(total)}"
                            updateNotification(updated.title, percent, text, isPaused = isPausedFlag.get())

                            val now = System.currentTimeMillis()
                            if (now - lastDbUpdateTime >= 2500L) {
                                lastDbUpdateTime = now
                                serviceScope.launch {
                                    dao.update(updated)
                                }
                            }
                        },
                        isPaused = { isPausedFlag.get() },
                        isCancelled = { isCancelledFlag.get() }
                    )

                    // Download completed successfully
                    val completedTask = runningTask.copy(
                        status = DownloadStatus.COMPLETED,
                        filePath = completedFile.absolutePath,
                        downloadedBytes = completedFile.length(),
                        totalBytes = completedFile.length(),
                        speedBytesPerSec = 0L
                    )
                    dao.update(completedTask)
                    _currentActiveTask.value = completedTask

                } catch (e: Exception) {
                    if (isCancelledFlag.get()) {
                        val cancelled = runningTask.copy(status = DownloadStatus.CANCELLED)
                        dao.update(cancelled)
                        _currentActiveTask.value = cancelled
                        break
                    } else {
                        val failed = runningTask.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = e.localizedMessage ?: "Bilinmeyen hata"
                        )
                        dao.update(failed)
                        _currentActiveTask.value = failed
                    }
                }

                delay(500)
            }

            stopForegroundTask()
        }
    }

    private fun startForegroundWithNotification(title: String, progress: Int, contentText: String) {
        val notification = buildNotification(title, progress, contentText, isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, progress: Int, contentText: String, isPaused: Boolean) {
        try {
            val notification = buildNotification(title, progress, contentText, isPaused)
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS permission on Android 13+
        }
    }

    private fun buildNotification(title: String, progress: Int, contentText: String, isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, DownloadService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = ACTION_CANCEL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            2,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeActionTitle = if (isPaused) getString(R.string.btn_resume) else getString(R.string.btn_pause)

        val builder = NotificationCompat.Builder(this, MimuApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText(getString(R.string.app_name))
            .setContentIntent(openAppPendingIntent)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, pauseResumeActionTitle, pauseResumePendingIntent)
            .addAction(0, getString(R.string.btn_stop), cancelPendingIntent)

        if (progress in 0..100) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(100, 0, true)
        }

        return builder.build()
    }

    private fun stopForegroundTask() {
        releaseWakeLock()
        _isServiceActive.value = false
        _isServicePaused.value = false
        _currentActiveTask.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours maximum timeout
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    private fun calculatePercent(downloaded: Long, total: Long): Int {
        if (total <= 0L) return 0
        return ((downloaded * 100) / total).toInt().coerceIn(0, 100)
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format(java.util.Locale.US, "%.0f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
