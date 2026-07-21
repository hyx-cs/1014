package com.example.deepseekwidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.deepseekwidget.worker.BalanceRefreshWorker

/**
 * 刷新按钮回调 —— 用户点击小组件上的刷新按钮时触发。
 *
 * 立即调度 [BalanceRefreshWorker] 执行一次余额查询，
 * 不等待定期任务（30 分钟间隔）。
 */
class RefreshCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        BalanceRefreshWorker.scheduleImmediate(context)
    }
}
