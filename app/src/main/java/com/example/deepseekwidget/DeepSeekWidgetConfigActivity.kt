package com.example.deepseekwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.deepseekwidget.worker.BalanceRefreshWorker

class DeepSeekWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_config)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val apiKeyInput = findViewById<EditText>(R.id.api_key_input)
        val saveButton = findViewById<Button>(R.id.save_button)

        // 回填已有 Key
        val existing = loadApiKey()
        if (!existing.isNullOrBlank()) {
            apiKeyInput.setText(existing)
            saveButton.isEnabled = true
        }

        apiKeyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveButton.isEnabled = !s.isNullOrBlank() && s.length >= 10
            }
        })

        saveButton.setOnClickListener {
            val key = apiKeyInput.text.toString().trim()
            if (key.isBlank()) return@setOnClickListener
            if (!key.startsWith("sk-")) {
                apiKeyInput.error = "API Key 应以 sk- 开头"
                return@setOnClickListener
            }
            saveApiKey(key)

            // 触发首次刷新
            BalanceRefreshWorker.scheduleImmediate(this)
            BalanceRefreshWorker.schedule(this)

            Toast.makeText(this, "已保存，正在查询余额…", Toast.LENGTH_SHORT).show()

            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, result)
            finish()
        }
    }

    private fun saveApiKey(key: String) {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                this, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putString(KEY_API_KEY, key).apply()
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadApiKey(): String? {
        return try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                this, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString(KEY_API_KEY, null)
        } catch (_: Exception) { null }
    }

    companion object {
        const val PREFS_NAME = "deepseek_widget_prefs"
        const val KEY_API_KEY = "api_key"
    }
}
