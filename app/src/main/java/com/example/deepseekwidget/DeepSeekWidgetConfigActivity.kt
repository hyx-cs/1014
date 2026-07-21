package com.example.deepseekwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.deepseekwidget.worker.BalanceRefreshWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * 小组件配置页面 —— 添加小组件时弹出。
 *
 * 用户在此页面输入 DeepSeek API Key，
 * 保存到 [EncryptedSharedPreferences] 加密存储。
 * 配置完成后立即触发一次余额查询并完成小组件添加。
 *
 * 流程:
 * 1. 长按桌面 → 添加小组件 → 选择 "DeepSeek 余额"
 * 2. 弹出此配置页
 * 3. 输入 API Key → 点击保存
 * 4. API Key 加密存储到本地
 * 5. 触发首次余额查询 → 小组件添加到桌面
 */
class DeepSeekWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)

        // 从 Intent 获取 widget ID
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // 无效 ID → 直接取消
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // 加载已有 API Key（如有）
        val existingKey = loadApiKey()

        apiKeyInput = findViewById(R.id.api_key_input)
        saveButton = findViewById(R.id.save_button)

        // 回填已有 Key
        if (!existingKey.isNullOrBlank()) {
            apiKeyInput.setText(existingKey)
        }

        // 输入监听 → 验证格式
        apiKeyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val key = s?.toString()?.trim() ?: ""
                saveButton.isEnabled = key.isNotBlank() && key.length >= 10
            }
        })

        // 保存按钮
        saveButton.setOnClickListener {
            val key = apiKeyInput.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                apiKeyInput.error = "API Key 不能为空"
                return@setOnClickListener
            }
            if (!key.startsWith("sk-")) {
                apiKeyInput.error = "API Key 应以 sk- 开头"
                return@setOnClickListener
            }

            saveApiKey(key)
            onConfigComplete()
        }

        // 初始按钮状态
        saveButton.isEnabled = !existingKey.isNullOrBlank()
    }

    /** 保存完成 → 触发首次刷新 + 返回小组件 ID */
    private fun onConfigComplete() {
        // 立即触发首次余额查询
        BalanceRefreshWorker.scheduleImmediate(this)
        // 调度定期刷新
        BalanceRefreshWorker.schedule(this)

        Toast.makeText(this, "API Key 已保存，正在查询余额…", Toast.LENGTH_SHORT).show()

        // 返回成功结果 → 小组件添加到桌面
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    /** 加密保存 API Key */
    private fun saveApiKey(key: String) {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                this,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putString(KEY_API_KEY, key).apply()
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /** 从加密存储加载已有 API Key */
    private fun loadApiKey(): String? {
        return try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                this,
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
        const val PREFS_NAME = "deepseek_widget_prefs"
        const val KEY_API_KEY = "api_key"
    }
}
