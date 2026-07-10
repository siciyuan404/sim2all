package com.sim2all.smsforward.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sim2all.smsforward.App
import com.sim2all.smsforward.service.ForwardService
import java.util.concurrent.TimeUnit

/**
 * 失败重试 Worker。
 *
 * 策略：
 * - 调度一个 OneTimeWorkRequest，延迟 backoffSeconds 后执行
 * - 执行时如果发现仍有 PENDING 队列，则启动 ForwardService 由它接管发送
 * - 多次调度通过唯一 workName 合并（KEEP 已存在的，避免重复触发）
 */
class RetryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val pending = app.db.pendingSmsDao()
            .listByStatus("PENDING", limit = 1)
        if (pending.isEmpty()) {
            return Result.success()
        }
        ForwardService.launch(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "retry_forward"

        fun schedule(context: Context, backoffSeconds: Long = 30) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<RetryWorker>()
                .setInitialDelay(backoffSeconds, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .setInputData(workDataOf("scheduled_at" to System.currentTimeMillis()))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                // 已存在则保留旧的，新请求被丢弃；防止短时间内被多次触发
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
