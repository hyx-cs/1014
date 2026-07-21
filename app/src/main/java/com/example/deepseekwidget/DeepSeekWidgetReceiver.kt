package com.example.deepseekwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.deepseekwidget.worker.BalanceRefreshWorker

class DeepSeekWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget = DeepSeekWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        BalanceRefreshWorker.schedule(context)
    }
}
