package com.example.deepseekwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.deepseekwidget.worker.BalanceRefreshWorker

/**
 * 小组件 BroadcastReceiver —— 使用传统 RemoteViews + AppWidgetProvider。
 * 不使用 Glance 渲染，避免 RemoteViews 兼容性问题。
 */
class DeepSeekWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val state = DeepSeekWidgetStateDefinition.readState(context)

        for (appWidgetId in appWidgetIds) {
            val views = buildRemoteViews(context, state, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        BalanceRefreshWorker.schedule(context)
    }

    private fun buildRemoteViews(context: Context, state: WidgetState, widgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val symbol = if (state.currency == "USD") "$" else "¥"

        if (state.isLoading) {
            views.setTextViewText(R.id.widget_title, "DeepSeek API")
            views.setTextViewText(R.id.widget_balance, "Loading...")
            views.setTextViewText(R.id.widget_detail, "")
            views.setTextViewText(R.id.widget_status, "⏳ Syncing")
        } else if (state.hasError && !state.hasData) {
            views.setTextViewText(R.id.widget_title, "DeepSeek API")
            views.setTextViewText(R.id.widget_balance, "⚠️ Error")
            views.setTextViewText(R.id.widget_detail, state.errorMessage ?: "")
            views.setTextViewText(R.id.widget_status, "Check API Key")
        } else {
            views.setTextViewText(R.id.widget_title, "🔷 DeepSeek API")
            views.setTextViewText(R.id.widget_balance, "$symbol ${state.totalBalance}")
            views.setTextViewText(
                R.id.widget_detail,
                "Granted: $symbol ${state.grantedBalance}  |  Top-up: $symbol ${state.toppedUpBalance}"
            )
            views.setTextViewText(
                R.id.widget_status,
                if (state.isAvailable) "✅ Active" else "❌ Inactive"
            )
        }

        // 刷新按钮 → 触发 onUpdate
        val intent = Intent(context, DeepSeekWidgetReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        }
        views.setOnClickPendingIntent(
            R.id.widget_refresh,
            PendingIntent.getBroadcast(
                context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return views
    }
}

private val WidgetState.hasData get() = totalBalance.isNotEmpty()
private val WidgetState.hasError get() = errorMessage != null
