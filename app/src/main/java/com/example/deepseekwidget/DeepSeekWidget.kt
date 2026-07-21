package com.example.deepseekwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class DeepSeekWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_title, "DeepSeek API")
                views.setTextViewText(R.id.widget_balance, "Setup Required")
                views.setTextViewText(R.id.widget_detail, "Add widget & enter API Key")
                views.setTextViewText(R.id.widget_status, "Ready")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            // fallback
        }
    }
}
