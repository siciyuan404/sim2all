package com.sim2all.smsforward.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat
import com.sim2all.smsforward.App
import com.sim2all.smsforward.data.PendingSms
import com.sim2all.smsforward.service.ForwardService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 监听系统 SMS_RECEIVED 广播。
 *
 * 在 Android 8+ 上 SMS_RECEIVED 仍允许 manifest 注册（属于豁免列表）。
 * 收到短信后：
 *  1. 解析出 sender 与 body
 *  2. 根据配置过滤规则判断是否需要转发
 *  3. 满足条件则入队，并启动 ForwardService
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "无 RECEIVE_SMS 权限，忽略")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        // 同一发送方的多段短信拼接
        val sender = messages[0].displayOriginatingAddress ?: messages[0].originatingAddress ?: "未知号码"
        val body = messages.joinToString("") { it.displayMessageBody ?: it.messageBody ?: "" }
        val receivedAt = System.currentTimeMillis()

        Log.i(TAG, "收到短信 from=$sender len=${body.length}")

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as App
                val cfg = app.settings.snapshot()

                if (!cfg.enabled) {
                    Log.d(TAG, "总开关未开启，跳过")
                    return@launch
                }
                if (!cfg.matches(sender, body)) {
                    Log.d(TAG, "未匹配过滤规则，跳过")
                    return@launch
                }
                if (cfg.recipients.isEmpty()) {
                    Log.w(TAG, "收件邮箱为空，跳过")
                    return@launch
                }

                app.db.pendingSmsDao().insert(
                    PendingSms(
                        sender = sender,
                        body = body,
                        receivedAt = receivedAt,
                        toAddress = cfg.toAddress
                    )
                )
                ForwardService.launch(context)
            } catch (t: Throwable) {
                Log.e(TAG, "处理短信异常", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
