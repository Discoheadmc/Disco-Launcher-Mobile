package com.discocock.launcher.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.discocock.launcher.DiscoApplication
import com.discocock.launcher.game.GameInstaller
import com.discocock.launcher.game.RuntimeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps downloads alive while the screen is off / the app is idle:
 * a foreground service with a partial wake lock (CPU stays up) and a
 * progress notification. Started and stopped automatically by
 * [com.discocock.launcher.game.DownloadManager].
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotify = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        // Must call startForeground promptly after startForegroundService().
        startForeground(NOTIFICATION_ID, buildNotification(0, ""))

        // Hold the CPU awake so downloads don't stall when the screen is off.
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L) // safety cap: 4 hours
            }

        val container = (application as DiscoApplication).container
        scope.launch {
            combine(
                container.downloads.active,
                container.installer.progress,
                container.runtimes.progress,
            ) { active, install, runtime -> Triple(active, install, runtime) }
                .collect { (active, install, runtime) ->
                    if (active.isEmpty()) {
                        stopSelf()
                        return@collect
                    }
                    // Ekran kapalıyken bile devam: ilerlemeyi bildirimde tut.
                    val now = System.currentTimeMillis()
                    if (now - lastNotify < 1000) return@collect
                    lastNotify = now
                    val progressText = when {
                        install is GameInstaller.Progress.Working ->
                            "${install.stage} — ${install.current}/${install.total}"
                        runtime is RuntimeManager.RuntimeProgress.Working ->
                            "JRE ${runtime.downloaded / (1024 * 1024)}/${runtime.total / (1024 * 1024)} MB — ${runtime.stage}"
                        else -> "${active.size} indirme sürüyor"
                    }
                    notifyProgress(active.size, progressText)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // onCreate ran startForeground once; re-starts just refresh the badge.
        val container = (application as DiscoApplication).container
        notifyProgress(container.downloads.active.value.size, "")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun notifyProgress(count: Int, progressText: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(count, progressText))
    }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Arka plan indirmeleri",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun buildNotification(count: Int, progressText: String): Notification {
        val text = progressText.ifBlank {
            if (count > 0) "$count indirme sürüyor" else "İndirmeler başladı"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Disco Launcher")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "disco_downloads"
        private const val NOTIFICATION_ID = 42
        private const val WAKE_LOCK_TAG = "discolauncher:downloads"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, DownloadService::class.java))
        }
    }
}
