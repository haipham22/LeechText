package dev.haipham22.leechtext

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.haipham22.leechtext.resources.Res
import dev.haipham22.leechtext.resources.notif_downloading_chapter
import dev.haipham22.leechtext.ui.BookPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Foreground service (MUST-P3 A1): download sách ở background, notification
 * tiến trình, không bị hệ thống giết khi app ở background (A2 một phần —
 * foreground service miễn nhiễm doze ở mức cơ bản).
 */
class DownloadService : Service() {
    companion object {
        const val CHANNEL_ID = "leechtext_download"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_URL = "url"
        const val EXTRA_CHAPTER_RANGE = "chapterRange"
        const val ACTION_CANCEL = "dev.haipham22.leechtext.action.CANCEL_DOWNLOAD"

        fun start(
            context: Context,
            url: String,
            chapterRange: String? = null,
        ) {
            val intent =
                Intent(context, DownloadService::class.java).apply {
                    putExtra(EXTRA_URL, url)
                    chapterRange?.let { putExtra(EXTRA_CHAPTER_RANGE, it) }
                }
            context.startForegroundService(intent)
        }

        /** Hủy download đang chạy — nút Hủy trên notification. */
        fun cancel(context: Context) {
            context.startService(
                Intent(context, DownloadService::class.java).setAction(ACTION_CANCEL),
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableSetOf<Job>()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("LeechText", 0, 0))
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_CANCEL) {
            jobs.forEach { it.cancel() }
            jobs.clear()
            stopSelf()
            return START_NOT_STICKY
        }
        val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val chapterRange = intent.getStringExtra(EXTRA_CHAPTER_RANGE) // null = full book
        val job =
            scope.launch {
                try {
                    updateNotification("Downloading: $url", 0, 0)
                    // Resolve 1 lần (suspend) — callback onProgress không phải suspend
                    val downloadingText = getString(Res.string.notif_downloading_chapter)
                    val koin = org.koin.mp.KoinPlatformTools.defaultContext().get()
                    val (_, summary) =
                        BookPipeline.downloadBook(
                            url,
                            chapterRange,
                            onProgress = { done, total ->
                                updateNotification(downloadingText, done, total)
                            },
                            log = koin.get<dev.haipham22.leechtext.log.EngineLogger>(),
                            pluginManager = koin.get<dev.haipham22.leechtext.plugin.PluginManager>(),
                        )
                    updateNotification(
                        "Done: ${summary.ok} new, ${summary.error} errors",
                        summary.total,
                        summary.total,
                    )
                } catch (e: Exception) {
                    updateNotification("Error: ${e.message ?: e::class.simpleName ?: "error"}", 0, 0)
                } finally {
                    stopSelf()
                }
            }
        job.invokeOnCompletion { jobs.remove(job) }
        jobs.add(job)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Download queue" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        text: String,
        progress: Int,
        total: Int,
    ): Notification {
        val cancelPi =
            PendingIntent.getService(
                this,
                0,
                Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val builder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("LeechText")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel",
                    cancelPi,
                )
        if (total > 0) {
            builder.setProgress(total, progress, false)
        }
        return builder.build()
    }

    private fun updateNotification(
        text: String,
        progress: Int,
        total: Int,
    ) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text, progress, total))
    }
}
