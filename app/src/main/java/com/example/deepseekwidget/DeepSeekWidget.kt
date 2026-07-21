package com.example.deepseekwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.deepseekwidget.worker.BalanceRefreshWorker

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

        when {
            state.isLoading -> {
                views.setTextViewText(R.id.widget_title, "DeepSeek API")
                views.setTextViewText(R.id.widget_balance, "⏳")
                views.setTextViewText(R.id.widget_status, "Loading")
                views.setTextViewText(R.id.widget_granted, "--")
                views.setTextViewText(R.id.widget_topped_up, "--")
                views.setTextViewText(R.id.widget_refresh_icon, "⏳")
                views.setTextViewText(R.id.widget_refresh_text, "Syncing...")
            }
            state.hasError && !state.hasData -> {
                views.setTextViewText(R.id.widget_title, "DeepSeek API")
                views.setTextViewText(R.id.widget_balance, "⚠️")
                views.setTextViewText(R.id.widget_status, state.errorMessage ?: "Error")
                views.setTextViewText(R.id.widget_granted, "--")
                views.setTextViewText(R.id.widget_topped_up, "--")
                views.setTextViewText(R.id.widget_refresh_icon, "🔄")
                views.setTextViewText(R.id.widget_refresh_text, "Retry")
            }
            state.hasData -> {
                views.setTextViewText(R.id.widget_title, "🔷 DeepSeek API")
                views.setTextViewText(R.id.widget_balance, "$symbol ${state.totalBalance}")
                views.setTextViewText(
                    R.id.widget_status,
                    if (state.isAvailable) "🟢 Active" else "🔴 Inactive"
                )
                views.setTextViewText(R.id.widget_granted, "$symbol ${state.grantedBalance}")
                views.setTextViewText(R.id.widget_topped_up, "$symbol ${state.toppedUpBalance}")
                views.setTextViewText(R.id.widget_refresh_icon, "🔄")
                views.setTextViewText(R.id.widget_refresh_text, "Refresh")
            }
            else -> {
                views.setTextViewText(R.id.widget_title, "🔷 DeepSeek API")
                views.setTextViewText(R.id.widget_balance, "Setup")
                views.setTextViewText(R.id.widget_status, "Ready")
                views.setTextViewText(R.id.widget_granted, "--")
                views.setTextViewText(R.id.widget_topped_up, "--")
                views.setTextViewText(R.id.widget_refresh_icon, "🔑")
                views.setTextViewText(R.id.widget_refresh_text, "Add Key")
            }
        }

        // 刷新按钮点击
        val refreshIntent = Intent(context, DeepSeekWidgetReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(widgetId))
        }
        views.setOnClickPendingIntent(
            R.id.widget_refresh,
            PendingIntent.getBroadcast(
                context, widgetId, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return views
    }
}

private val WidgetState.hasData get() = totalBalance.isNotEmpty()
private val WidgetState.hasError get() = errorMessage != null
