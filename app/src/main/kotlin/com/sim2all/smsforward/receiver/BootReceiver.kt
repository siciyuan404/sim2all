package com.sim2all.smsforward.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sim2all.smsforward.App
import com.sim2all.smsforward.service.ForwardService
import com.sim2all.smsforward.worker.RetryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机自启：开机后扫描待发队列，若有积压则启动转发服务。
 *
 * 同时监听 MY_PACKAGE_REPLACED，确保应用升级后未发出的短信仍会被处理。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "收到开机/升级广播：${intent.action}")
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as App
                        val list = app.db.pendingSmsDao()
                            .listByStatus("PENDING", limit = 1)
                        if (list.isNotEmpty()) {
                            RetryWorker.schedule(context, backoffSeconds = 15)
                            ForwardService.launch(context)
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "自启异常", t)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
