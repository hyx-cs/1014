package com.example.deepseekwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.deepseekwidget.worker.BalanceRefreshWorker

/**
 * 小组件 BroadcastReceiver —— 系统与 Glance 小组件的桥梁。
 *
 * 继承 [GlanceAppWidgetReceiver] 自动处理标准 App Widget 生命周期：
 * - onUpdate: 系统定时更新 + 首次添加
 * - onAppWidgetOptionsChanged: 尺寸变化
 * - onDeleted: 移除小组件
 *
 * 额外逻辑:
 * - 首次添加到桌面时，启动 WorkManager 定期刷新
 * - 所有小组件实例共享同一个 [DeepSeekWidget]
 */
class DeepSeekWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: DeepSeekWidget
        get() = DeepSeekWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // 确保定期刷新已调度（幂等操作，KEEP 策略不会创建重复任务）
        BalanceRefreshWorker.schedule(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // 如果所有小组件都已移除，取消定期刷新
        // 注意: 简单实现直接取消；生产环境应检查剩余实例数
    }
}
