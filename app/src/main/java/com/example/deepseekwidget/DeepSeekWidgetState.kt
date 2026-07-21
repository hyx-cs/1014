package com.example.deepseekwidget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * 小组件状态 —— 由 WorkManager 后台更新，Glance UI 消费。
 *
 * @property isLoading 是否正在加载（首次添加小组件或刷新中）
 * @property isAvailable 账户是否可用（DeepSeek API 返回的 is_available）
 * @property totalBalance 总余额字符串（如 "110.00"）
 * @property grantedBalance 赠金余额
 * @property toppedUpBalance 充值余额
 * @property currency 币种 (CNY / USD)
 * @property errorMessage 错误消息，非 null 时表示最近一次请求出错
 * @property lastUpdated 上次成功更新时间戳 (epoch millis)
 */
@Serializable
data class WidgetState(
    val isLoading: Boolean = true,
    val isAvailable: Boolean = false,
    val totalBalance: String = "",
    val grantedBalance: String = "",
    val toppedUpBalance: String = "",
    val currency: String = "CNY",
    val errorMessage: String? = null,
    val lastUpdated: Long = 0L
)

/**
 * [WidgetState] 的 DataStore 序列化器。
 *
 * 使用 JSON 格式存储到文件，保证可读性和跨版本兼容。
 * 解析失败时返回默认空状态，防止文件损坏导致小组件崩溃。
 */
private object WidgetStateSerializer : Serializer<WidgetState> {

    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: WidgetState
        get() = WidgetState()

    override suspend fun readFrom(input: InputStream): WidgetState {
        return try {
            val text = input.bufferedReader().use { it.readText() }
            if (text.isBlank()) defaultValue
            else json.decodeFromString<WidgetState>(text)
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: WidgetState, output: OutputStream) {
        output.bufferedWriter().use { writer ->
            writer.write(json.encodeToString(t))
        }
    }
}

/**
 * Glance 状态定义 —— 用于在 Glance 组件中读取/写入 [WidgetState]。
 *
 * 所有小组件实例共享同一份状态（同一 API Key 的余额数据），
 * 因此忽略 [fileKey] 参数，始终使用固定文件名。
 */
object DeepSeekWidgetStateDefinition : GlanceStateDefinition<WidgetState> {

    private const val STATE_FILE_NAME = "deepseek_widget_state.json"

    @Volatile
    private var instance: DataStore<WidgetState>? = null

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<WidgetState> {
        return instance ?: synchronized(this) {
            instance ?: DataStoreFactory.create(
                serializer = WidgetStateSerializer,
                produceFile = {
                    context.dataDir.resolve(STATE_FILE_NAME)
                }
            ).also { instance = it }
        }
    }
}
