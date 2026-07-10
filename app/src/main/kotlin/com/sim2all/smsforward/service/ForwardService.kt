package com.sim2all.smsforward.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sim2all.smsforward.App
import com.sim2all.smsforward.MainActivity
import com.sim2all.smsforward.R
import com.sim2all.smsforward.data.AppDb
import com.sim2all.smsforward.data.PendingSms
import com.sim2all.smsforward.data.PendingSmsDao
import com.sim2all.smsforward.data.Settings
import com.sim2all.smsforward.mail.MailSender
import com.sim2all.smsforward.worker.RetryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date

/**
 * 前台服务：负责把待发送队列里的短信依次发出去。
 *
 * 调用方式：
 *   startService(Intent(context, ForwardService::class.java))
 *
 * 流程：
 *  1. 启动 → 升为前台服务（带通知）
 *  2. 取出所有 PENDING 记录，逐条发送
 *  3. 成功 → 状态置为 SENT
 *     失败 → retryCount+1，未超过上限则置回 PENDING；超过上限则置为 FAILED
 *  4. 队列空 → 退出前台并停止
 */
class ForwardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startForeground(NOTIF_ID, buildNotification("准备发送…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { drainQueue() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun drainQueue() {
        val dao = AppDb.get(this).pendingSmsDao()
        // 把上次崩溃残留的 SENDING 复位
        dao.resetStuckSending()

        val cfg = App.instance.settings.snapshot()
        if (!cfg.enabled) {
            Log.i(TAG, "已禁用，跳过发送")
            stopSelfSafely()
            return
        }
        if (cfg.recipients.isEmpty()) {
            Log.w(TAG, "收件邮箱为空，跳过")
            stopSelfSafely()
            return
        }

        val sender = try { MailSender(cfg) } catch (t: Throwable) {
            Log.e(TAG, "MailSender 构造失败", t)
            stopSelfSafely()
            return
        }

        var processed = 0
        while (true) {
            val list = dao.listByStatus(PendingSms.STATUS_PENDING, limit = 1)
            if (list.isEmpty()) break
            val item = list.first()
            sendOne(dao, sender, item)
            processed++
        }
        Log.i(TAG, "本轮处理 $processed 条")
        stopSelfSafely()
    }

    private suspend fun sendOne(dao: PendingSmsDao, sender: MailSender, item: PendingSms) {
        // 占位为 SENDING
        dao.update(item.copy(status = PendingSms.STATUS_SENDING, lastAttemptAt = System.currentTimeMillis()))
        notifyProgress(item)

        try {
            val recipients = item.toAddress
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            sender.send(
                sender = item.sender,
                body = item.body,
                receivedAt = Date(item.receivedAt),
                recipients = recipients
            )
            dao.update(item.copy(
                status = PendingSms.STATUS_SENT,
                lastError = null,
                sentAt = System.currentTimeMillis()
            ))
        } catch (t: Throwable) {
            Log.w(TAG, "发送失败 id=${item.id}: ${t.message}")
            val newRetry = item.retryCount + 1
            val overLimit = newRetry >= App.instance.settings.snapshot().maxRetry
            dao.update(item.copy(
                status = if (overLimit) PendingSms.STATUS_FAILED else PendingSms.STATUS_PENDING,
                retryCount = newRetry,
                lastError = t.message?.take(500),
                lastAttemptAt = System.currentTimeMillis()
            ))
            if (!overLimit) {
                // 调度指数退避重试
                RetryWorker.schedule(this, backoffSeconds = (1L shl newRetry.coerceAtMost(6)))
            }
        }
    }

    private fun notifyProgress(item: PendingSms) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification("正在发送：来自 ${item.sender}"))
    }

    private fun stopSelfSafely() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sms)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "短信转发服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示当前转发进度"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    companion object {
        private const val TAG = "ForwardService"
        private const val CHANNEL_ID = "forward_service"
        private const val NOTIF_ID = 1001

        fun launch(context: Context) {
            val intent = Intent(context, ForwardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
