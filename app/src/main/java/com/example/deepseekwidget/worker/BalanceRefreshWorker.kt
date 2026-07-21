package com.example.deepseekwidget.worker

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.*
import com.example.deepseekwidget.DeepSeekWidget
import com.example.deepseekwidget.DeepSeekWidgetStateDefinition
import com.example.deepseekwidget.WidgetState
import com.example.deepseekwidget.network.ApiException
import com.example.deepseekwidget.network.DeepSeekApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 余额刷新 Worker —— 由 WorkManager 调度执行。
 *
 * 执行流程：
 * 1. 从 EncryptedSharedPreferences 读取 API Key
 * 2. 调用 DeepSeek API 查询余额
 * 3. 将结果写入 Glance DataStore 持久化状态
 * 4. 触发小组件 UI 刷新 ([DeepSeekWidget.updateAll])
 *
 * 调度方式：
 * - 定期: [schedule] — 每 30 分钟自动执行
 * - 立即: [scheduleImmediate] — 用户点击刷新按钮或首次添加小组件时
 * - 取消: [cancel]
 */
class BalanceRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val apiService = DeepSeekApiService()

    override suspend fun doWork(): Result {
        val apiKey = getApiKey(applicationContext)

        if (apiKey.isNullOrBlank()) {
            persistState(
                WidgetState(
                    isLoading = false,
                    errorMessage = "API Key 未设置"
                )
            )
            notifyWidget(applicationContext)
            return Result.failure()
        }

        val result = withContext(Dispatchers.IO) {
            apiService.getBalance(apiKey)
        }

        return result.fold(
            onSuccess = { response ->
                val info = response.balanceInfos.firstOrNull()
                persistState(
                    WidgetState(
                        isLoading = false,
                        isAvailable = response.isAvailable,
                        totalBalance = info?.totalBalance ?: "0.00",
                        grantedBalance = info?.grantedBalance ?: "0.00",
                        toppedUpBalance = info?.toppedUpBalance ?: "0.00",
                        currency = info?.currency ?: "CNY",
                        errorMessage = null,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
                notifyWidget(applicationContext)
                Result.success()
            },
            onFailure = { error ->
                // 保留上次成功的数据，仅更新错误信息和加载状态
                val dataStore = DeepSeekWidgetStateDefinition.getDataStore(
                    applicationContext, SHARED_FILE_KEY
                )
                val current = try {
                    dataStore.data.first()
                } catch (_: Exception) {
                    WidgetState()
                }
                persistState(
                    current.copy(
                        isLoading = false,
                        errorMessage = (error as? ApiException)?.message ?: error.message
                            ?: "未知错误"
                    )
                )
                notifyWidget(applicationContext)
                // 网络临时故障，稍后重试
                Result.retry()
            }
        )
    }

    /** 将状态写入 DataStore */
    private suspend fun persistState(state: WidgetState) {
        try {
            val dataStore = DeepSeekWidgetStateDefinition.getDataStore(
                applicationContext, SHARED_FILE_KEY
            )
            dataStore.updateData { state }
        } catch (_: Exception) {
            // 状态持久化失败时不崩溃 Worker
        }
    }

    /** 触发小组件 UI 刷新 */
    private suspend fun notifyWidget(context: Context) {
        try {
            DeepSeekWidget().updateAll(context)
        } catch (_: Exception) {
            // 小组件更新失败不重试
        }
    }

    /** 从加密存储读取 API Key */
    private fun getApiKey(context: Context): String? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString(KEY_API_KEY, null)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val WORK_NAME_PERIODIC = "deepseek_balance_refresh_periodic"
        const val SHARED_FILE_KEY = "shared"

        // EncryptedSharedPreferences 标识
        private const val PREFS_NAME = "deepseek_widget_prefs"
        private const val KEY_API_KEY = "api_key"

        /**
         * 调度定期刷新 —— 每 30 分钟执行一次。
         * 使用 KEEP 策略避免重复调度。
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BalanceRefreshWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * 立即执行一次刷新（手动触发）。
         */
        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<BalanceRefreshWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /** 取消所有定期刷新任务 */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        }
    }
}
