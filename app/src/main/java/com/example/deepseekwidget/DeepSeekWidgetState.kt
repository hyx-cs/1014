package com.example.deepseekwidget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.io.OutputStream

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

/** JSON 序列化器 */
private val json = Json { ignoreUnknownKeys = true }

/** DataStore Serializer */
private object StateSerializer : Serializer<WidgetState> {
    override val defaultValue: WidgetState get() = WidgetState()
    override suspend fun readFrom(input: InputStream): WidgetState {
        return try {
            json.decodeFromString(input.bufferedReader().readText())
        } catch (_: Exception) { defaultValue }
    }
    override suspend fun writeTo(t: WidgetState, output: OutputStream) {
        output.bufferedWriter().use { it.write(json.encodeToString(t)) }
    }
}

/**
 * Glance 状态定义 —— 实现 getLocation 和 getDataStore。
 * Glance 1.1.0 要求同时实现两个抽象方法。
 */
object DeepSeekWidgetStateDefinition : GlanceStateDefinition<WidgetState> {

    private const val STATE_FILE = "deepseek_widget_state.json"

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, STATE_FILE)
    }

    @Volatile
    private var dataStore: DataStore<WidgetState>? = null

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<WidgetState> {
        return dataStore ?: synchronized(this) {
            dataStore ?: DataStoreFactory.create(
                serializer = StateSerializer,
                produceFile = { getLocation(context, fileKey) }
            ).also { dataStore = it }
        }
    }

    /** 便捷写入（WorkManager 调用） */
    suspend fun writeState(context: Context, state: WidgetState) {
        try {
            getDataStore(context, "shared").updateData { state }
        } catch (_: Exception) {}
    }

    /** 便捷读取（Glance 组件调用） */
    fun readState(context: Context): WidgetState {
        return try {
            val file = getLocation(context, "shared")
            if (file.exists()) json.decodeFromString(file.readText())
            else WidgetState()
        } catch (_: Exception) { WidgetState() }
    }
}
