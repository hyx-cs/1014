package com.example.deepseekwidget

import android.content.Context
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 小组件状态 —— 由 WorkManager 后台更新，Glance UI 消费。
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
 * Glance 状态定义 —— 使用 JSON 文件持久化 [WidgetState]。
 *
 * Glance 1.1.0 的 GlanceStateDefinition 需要实现 getLocation()。
 * 状态 JSON 文件位于 app 内部存储目录，所有小组件实例共享。
 */
object DeepSeekWidgetStateDefinition : GlanceStateDefinition<WidgetState> {

    private const val STATE_FILE_NAME = "deepseek_widget_state.json"

    private val json = Json { ignoreUnknownKeys = true }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, STATE_FILE_NAME)
    }

    /** 将 WidgetState 写入 JSON 文件（由 WorkManager 调用） */
    suspend fun writeState(context: Context, state: WidgetState) {
        try {
            val file = getLocation(context, "shared")
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(state))
        } catch (_: Exception) {
        }
    }

    /** 从 JSON 文件读取 WidgetState（由 Glance 组件调用） */
    fun readState(context: Context): WidgetState {
        return try {
            val file = getLocation(context, "shared")
            if (file.exists()) {
                json.decodeFromString<WidgetState>(file.readText())
            } else {
                WidgetState()
            }
        } catch (_: Exception) {
            WidgetState()
        }
    }
}
