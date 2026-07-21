package com.example.deepseekwidget.worker

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.*
import com.example.deepseekwidget.DeepSeekWidget
import com.example.deepseekwidget.DeepSeekWidgetStateDefinition
import com.example.deepseekwidget.WidgetState
import com.example.deepseekwidget.network.ApiException
import com.example.deepseekwidget.network.DeepSeekApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BalanceRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val apiService = DeepSeekApiService()

    override suspend fun doWork(): Result {
        val apiKey = getApiKey(applicationContext)

        if (apiKey.isNullOrBlank()) {
            DeepSeekWidgetStateDefinition.writeState(
                applicationContext,
                WidgetState(isLoading = false, errorMessage = "API Key not set")
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
                DeepSeekWidgetStateDefinition.writeState(
                    applicationContext,
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
                val current = DeepSeekWidgetStateDefinition.readState(applicationContext)
                DeepSeekWidgetStateDefinition.writeState(
                    applicationContext,
                    current.copy(
                        isLoading = false,
                        errorMessage = (error as? ApiException)?.message
                            ?: error.message ?: "Unknown error"
                    )
                )
                notifyWidget(applicationContext)
                Result.retry()
            }
        )
    }

    private suspend fun notifyWidget(context: Context) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(DeepSeekWidget::class.java)
            glanceIds.forEach { DeepSeekWidget().update(context, it) }
        } catch (_: Exception) {
        }
    }

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
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val WORK_NAME_PERIODIC = "deepseek_balance_refresh_periodic"
        private const val PREFS_NAME = "deepseek_widget_prefs"
        private const val KEY_API_KEY = "api_key"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<BalanceRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<BalanceRefreshWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        }
    }
}
