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
import androidx.glance.layout.VerticalAlignment.Companion.CenterVertically
import androidx.glance.layout.VerticalAlignment.Companion.Top
import androidx.glance.layout.HorizontalAlignment.Companion.Center
import androidx.glance.layout.HorizontalAlignment.Companion.SpaceBetween
import androidx.glance.layout.HorizontalAlignment.Companion.SpaceEvenly
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.deepseekwidget.ui.GlassColors

class DeepSeekWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val state = DeepSeekWidgetStateDefinition.readState(context)
                WidgetContent(state, actionRunCallback<RefreshCallback>())
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════

@Composable
private fun WidgetContent(state: WidgetState, onRefresh: Action) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.drawable.widget_background)
            .cornerRadius(24)
            .padding(16)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Top
        ) {
            HeaderRow(state)
            Spacer(GlanceModifier.height(10))

            when {
                state.isLoading -> LoadingContent()
                state.errorMessage != null && state.totalBalance.isEmpty() ->
                    ErrorContent(state.errorMessage ?: "Error")
                else -> BalanceContent(state)
            }

            Spacer(GlanceModifier.defaultWeight())
            BottomRow(state, onRefresh)
        }
    }
}

// ═══════════════════════════════════════════════════════════════

@Composable
private fun HeaderRow(state: WidgetState) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = SpaceBetween,
        verticalAlignment = CenterVertically
    ) {
        Text(
            text = "🔷 DeepSeek API",
            style = TextStyle(color = GlassColors.textPrimary, fontWeight = FontWeight.Medium)
        )
        Text(
            text = when {
                state.isLoading -> "⏳ sync..."
                state.isAvailable -> "🟢 online"
                else -> "🔴 offline"
            },
            style = TextStyle(color = GlassColors.textTertiary)
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = GlanceModifier.fillMaxWidth().padding(12),
        horizontalAlignment = Center
    ) {
        Text("⏳ Loading...", style = TextStyle(color = GlassColors.statusLoading))
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = GlanceModifier.fillMaxWidth().padding(12),
        horizontalAlignment = Center
    ) {
        Text("⚠️", style = TextStyle(fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(4))
        Text(message, style = TextStyle(color = GlassColors.textSecondary), maxLines = 2)
    }
}

@Composable
private fun BalanceContent(state: WidgetState) {
    val symbol = if (state.currency == "USD") "$" else "¥"

    Column(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Center
    ) {
        Text(
            text = "$symbol ${state.totalBalance}",
            style = TextStyle(color = GlassColors.textPrimary, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(2))
        Text("Balance", style = TextStyle(color = GlassColors.textTertiary))

        Spacer(GlanceModifier.height(12))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = SpaceEvenly
        ) {
            BalanceChip("Granted", state.grantedBalance, symbol)
            BalanceChip("Top-up", state.toppedUpBalance, symbol)
        }

        if (state.lastUpdated > 0L) {
            Spacer(GlanceModifier.height(8))
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
            .cornerRadius(12)
            .padding(horizontal = 14, vertical = 6),
        horizontalAlignment = Center
    ) {
        Text(
            text = "$symbol $amount",
            style = TextStyle(color = GlassColors.textPrimary, fontWeight = FontWeight.Medium)
        )
        Spacer(GlanceModifier.height(1))
        Text(label, style = TextStyle(color = GlassColors.textTertiary))
    }
}

@Composable
private fun BottomRow(state: WidgetState, onRefresh: Action) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = SpaceBetween,
        verticalAlignment = CenterVertically
    ) {
        when {
            state.isLoading -> Text(
                text = "⏳ Syncing",
                style = TextStyle(color = GlassColors.textTertiary)
            )
            state.hasError && !state.hasData -> Text(
                text = "⚠️ ${state.errorMessage ?: "Error"}",
                style = TextStyle(color = GlassColors.statusUnavailable),
                maxLines = 1
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
            modifier = GlanceModifier.clickable(onRefresh),
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

private val WidgetState.hasData get() = totalBalance.isNotEmpty()
private val WidgetState.hasError get() = errorMessage != null
