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
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.Dp
import com.example.deepseekwidget.ui.GlassColors

class DeepSeekWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = DeepSeekWidgetStateDefinition.readState(context)
                WidgetContent(
                    state = state,
                    onRefreshAction = actionRunCallback<RefreshCallback>()
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 主布局
// ═══════════════════════════════════════════════════════════════

@Composable
private fun WidgetContent(state: WidgetState, onRefreshAction: Action) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.drawable.widget_background)
            .cornerRadius(Dp(24f))
            .padding(Dp(16f))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            HeaderRow(state.isLoading, state.isAvailable)
            Spacer(GlanceModifier.height(Dp(10f)))

            when {
                state.isLoading -> LoadingContent()
                state.errorMessage != null && state.totalBalance.isEmpty() ->
                    ErrorContent(state.errorMessage ?: "Error")
                else -> BalanceContent(state)
            }

            Spacer(GlanceModifier.defaultWeight())

            BottomRow(state, onRefreshAction)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 子组件
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeaderRow(isLoading: Boolean, isAvailable: Boolean) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.SpaceBetween,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "🔷 DeepSeek API",
            style = TextStyle(
                color = GlassColors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        )
        val label = when {
            isLoading -> "⏳ sync..."
            isAvailable -> "🟢 online"
            else -> "🔴 offline"
        }
        Text(text = label, style = TextStyle(color = GlassColors.textTertiary))
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = GlanceModifier.fillMaxWidth().padding(Dp(12f)),
        horizontalAlignment = Alignment.Horizontal.Center
    ) {
        Text(
            text = "⏳ Loading...",
            style = TextStyle(color = GlassColors.statusLoading)
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = GlanceModifier.fillMaxWidth().padding(Dp(12f)),
        horizontalAlignment = Alignment.Horizontal.Center
    ) {
        Text(text = "⚠️", style = TextStyle(fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(Dp(4f)))
        Text(
            text = message,
            style = TextStyle(color = GlassColors.textSecondary),
            maxLines = 2
        )
    }
}

@Composable
private fun BalanceContent(state: WidgetState) {
    val symbol = if (state.currency == "USD") "$" else "¥"

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.Center
    ) {
        Text(
            text = "$symbol ${state.totalBalance}",
            style = TextStyle(
                color = GlassColors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(Dp(2f)))
        Text(text = "Balance", style = TextStyle(color = GlassColors.textTertiary))

        Spacer(GlanceModifier.height(Dp(12f)))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.SpaceEvenly
        ) {
            BalanceChip("Granted", state.grantedBalance, symbol)
            BalanceChip("Top-up", state.toppedUpBalance, symbol)
        }

        if (state.lastUpdated > 0L) {
            Spacer(GlanceModifier.height(Dp(8f)))
            Text(
                text = formatLastUpdated(state.lastUpdated),
                style = TextStyle(color = GlassColors.textTertiary),
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
            .cornerRadius(Dp(12f))
            .padding(horizontal = Dp(14f), vertical = Dp(6f)),
        horizontalAlignment = Alignment.Horizontal.Center
    ) {
        Text(
            text = "$symbol $amount",
            style = TextStyle(
                color = GlassColors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(GlanceModifier.height(Dp(1f)))
        Text(text = label, style = TextStyle(color = GlassColors.textTertiary))
    }
}

@Composable
private fun BottomRow(state: WidgetState, onRefreshAction: Action) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.SpaceBetween,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        when {
            state.isLoading -> Text(
                text = "⏳ Syncing",
                style = TextStyle(color = GlassColors.textTertiary)
            )
            state.hasData && state.errorMessage != null -> Text(
                text = "⚠️ Cached",
                style = TextStyle(color = GlassColors.statusLoading)
            )
            state.hasData -> Text(
                text = if (state.isAvailable) "✅ Active" else "❌ Inactive",
                style = TextStyle(
                    color = if (state.isAvailable) GlassColors.statusAvailable
                    else GlassColors.statusUnavailable,
                    fontWeight = FontWeight.Medium
                )
            )
            else -> Text(
                text = "⚙️ Setup",
                style = TextStyle(color = GlassColors.textSecondary)
            )
        }

        Text(
            text = "🔄 Refresh",
            modifier = GlanceModifier.clickable(onRefreshAction),
            style = TextStyle(color = GlassColors.textSecondary)
        )
    }
}

private fun formatLastUpdated(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        else -> "${diff / 86_400_000L}d ago"
    }
}

// Helpers for WidgetState
private val WidgetState.hasData get() = totalBalance.isNotEmpty()
