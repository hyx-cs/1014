package com.example.deepseekwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.FontSize
import androidx.glance.unit.dp
import com.example.deepseekwidget.ui.GlassColors

/**
 * DeepSeek 余额桌面小组件 — Glance 实现。
 *
 * 使用 Jetpack Glance 声明式 UI 渲染。
 * 状态来源于 [DeepSeekWidgetStateDefinition] DataStore，
 * 由 [BalanceRefreshWorker] 在后台更新。
 *
 * 视觉设计: iOS 毛玻璃风格 (Glass Morphism)
 * - 半透明白色背景叠加多层渐变 (widget_background.xml)
 * - 细腻边框模拟玻璃折光
 * - 顶部高光模拟光源反射
 */
class DeepSeekWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = currentState(DeepSeekWidgetStateDefinition)
                WidgetContent(
                    state = state,
                    onRefreshAction = actionRunCallback<RefreshCallback>()
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 主布局
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun WidgetContent(
    state: WidgetState,
    onRefreshAction: Action
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.drawable.widget_background)
            .cornerRadius(24.dp)
            .padding(16.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = VerticalAlignment.Top
        ) {
            // ── 标题行 ──────────────────────────
            HeaderRow(
                isLoading = state.isLoading,
                isAvailable = state.isAvailable
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            // ── 余额主体区域 ────────────────────
            when {
                state.isLoading -> LoadingContent()
                state.errorMessage != null && state.totalBalance.isEmpty() ->
                    ErrorContent(state.errorMessage ?: "未知错误")
                else -> BalanceContent(state)
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // ── 底部状态栏 ──────────────────────
            BottomRow(
                isAvailable = state.isAvailable,
                isLoading = state.isLoading,
                hasData = state.totalBalance.isNotEmpty(),
                errorMessage = state.errorMessage,
                onRefreshAction = onRefreshAction
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// 子组件
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun HeaderRow(isLoading: Boolean, isAvailable: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = HorizontalAlignment.SpaceBetween,
        verticalAlignment = VerticalAlignment.CenterVertically
    ) {
        Text(
            text = "🔷 DeepSeek API",
            style = TextStyle(
                color = GlassColors.textPrimary,
                fontSize = FontSize(13f),
                fontWeight = FontWeight.Medium
            )
        )
        // 状态指示灯
        val (dot, label) = when {
            isLoading -> "⏳" to "同步中"
            isAvailable -> "🟢" to "在线"
            else -> "🔴" to "离线"
        }
        Text(
            text = "$dot $label",
            style = TextStyle(
                color = GlassColors.textTertiary,
                fontSize = FontSize(11f)
            )
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = HorizontalAlignment.Center
    ) {
        Text(
            text = "⏳ 加载中…",
            style = TextStyle(
                color = GlassColors.statusLoading,
                fontSize = FontSize(14f)
            )
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = HorizontalAlignment.Center
    ) {
        Text(
            text = "⚠️",
            style = TextStyle(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = message,
            style = TextStyle(
                color = GlassColors.textSecondary,
                fontSize = FontSize(13f)
            ),
            maxLines = 2
        )
    }
}

@Composable
private fun BalanceContent(state: WidgetState) {
    val symbol = when (state.currency) {
        "USD" -> "$"
        else -> "¥"
    }

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = HorizontalAlignment.Center
    ) {
        // 大号余额数字
        Text(
            text = "$symbol ${state.totalBalance}",
            style = TextStyle(
                color = GlassColors.textPrimary,
                fontSize = FontSize(32f),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "总余额",
            style = TextStyle(
                color = GlassColors.textTertiary,
                fontSize = FontSize(12f)
            )
        )

        Spacer(modifier = GlanceModifier.height(12.dp))

        // 细分信息：赠金 / 充值
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = HorizontalAlignment.SpaceEvenly
        ) {
            BalanceChip(label = "赠金", amount = state.grantedBalance, symbol = symbol)
            BalanceChip(label = "充值", amount = state.toppedUpBalance, symbol = symbol)
        }

        // 最后更新时间
        if (state.lastUpdated > 0) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = formatLastUpdated(state.lastUpdated),
                style = TextStyle(
                    color = GlassColors.textTertiary,
                    fontSize = FontSize(10f)
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BalanceChip(label: String, amount: String, symbol: String) {
    Column(
        modifier = GlanceModifier
            .background(GlassColors.itemBackground)
            .cornerRadius(12.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = HorizontalAlignment.Center
    ) {
        Text(
            text = "$symbol $amount",
            style = TextStyle(
                color = GlassColors.textPrimary,
                fontSize = FontSize(15f),
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = GlanceModifier.height(1.dp))
        Text(
            text = label,
            style = TextStyle(
                color = GlassColors.textTertiary,
                fontSize = FontSize(11f)
            )
        )
    }
}

@Composable
private fun BottomRow(
    isAvailable: Boolean,
    isLoading: Boolean,
    hasData: Boolean,
    errorMessage: String?,
    onRefreshAction: Action
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = HorizontalAlignment.SpaceBetween,
        verticalAlignment = VerticalAlignment.CenterVertically
    ) {
        // 左侧：可用状态文字
        when {
            isLoading -> Text(
                text = "⏳ 查询中",
                style = TextStyle(
                    color = GlassColors.textTertiary,
                    fontSize = FontSize(11f)
                )
            )
            hasData && errorMessage != null -> Text(
                text = "⚠️ 显示缓存",
                style = TextStyle(
                    color = GlassColors.statusLoading,
                    fontSize = FontSize(11f)
                )
            )
            hasData -> Text(
                text = if (isAvailable) "✅ 可用" else "❌ 不可用",
                style = TextStyle(
                    color = if (isAvailable) GlassColors.statusAvailable
                    else GlassColors.statusUnavailable,
                    fontSize = FontSize(11f),
                    fontWeight = FontWeight.Medium
                )
            )
            else -> Text(
                text = "⚙️ 待配置",
                style = TextStyle(
                    color = GlassColors.textSecondary,
                    fontSize = FontSize(11f)
                )
            )
        }

        // 右侧：刷新按钮
        Text(
            text = "🔄 刷新",
            modifier = GlanceModifier.clickable(onRefreshAction),
            style = TextStyle(
                color = GlassColors.textSecondary,
                fontSize = FontSize(11f)
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════════════════════════

/** 格式化最后更新时间为人类可读的相对时间 */
private fun formatLastUpdated(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚更新"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前更新"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前更新"
        else -> "${diff / 86_400_000} 天前更新"
    }
}
