package com.example.deepseekwidget.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * DeepSeek API 客户端 —— 余额查询。
 *
 * 单一职责：通过 Bearer Token 认证调用 /user/balance 端点，
 * 返回解析后的 [BalanceResponse] 或错误信息。
 */
class DeepSeekApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 查询当前 API Key 对应的账户余额。
     *
     * @param apiKey DeepSeek API Key (sk-...)
     * @return 成功时 [Result.success] 包含 [BalanceResponse]，失败时 [Result.failure]
     */
    suspend fun getBalance(apiKey: String): Result<BalanceResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.deepseek.com/user/balance")
                    .header("Authorization", "Bearer $apiKey")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                val body = response.body?.string()
                    ?: throw ApiException("响应体为空")

                if (!response.isSuccessful) {
                    throw ApiException("HTTP ${response.code}: $body")
                }

                val balanceResponse = json.decodeFromString<BalanceResponse>(body)
                Result.success(balanceResponse)
            } catch (e: ApiException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(ApiException("网络请求失败: ${e.message}", e))
            }
        }
    }
}

/**
 * API 异常，包含可读的错误消息。
 */
class ApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
