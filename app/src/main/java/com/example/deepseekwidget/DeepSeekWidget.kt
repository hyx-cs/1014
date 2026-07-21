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

// ── 毛玻璃模拟背景色 ──
private val GLASS_BG = ColorProvider(0x26FFFFFF.toInt())
private val GLASS_BORDER = ColorProvider(0x40FFFFFF.toInt())

@Composable
private fun WidgetContent(state: WidgetState, onRefresh: Action) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GLASS_BG)
            .cornerRadius(24)
            .padding(16)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            HeaderRow(state)
            Box(GlanceModifier.height(10)) {}
            BodySection(state)
            Box(GlanceModifier.defaultWeight()) {}
            BottomRow(state, onRefresh)
        }
    }
}

@Composable
private fun HeaderRow(state: WidgetState) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = "🔷 DeepSeek API",
            style = TextStyle(color = GlassColors.textPrimary, fontWeight = FontWeight.Medium),
            modifier = GlanceModifier.defaultWeight()
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
private fun BodySection(state: WidgetState) {
    when {
        state.isLoading -> LoadingBlock()
        state.hasError && !state.hasData -> ErrorBlock(state.errorMessage ?: "Error")
        else -> BalanceBlock(state)
    }
}

@Composable
private fun LoadingBlock() {
    Box(modifier = GlanceModifier.fillMaxWidth().padding(12)) {
        Text("⏳ Loading...", style = TextStyle(color = GlassColors.statusLoading))
    }
}

@Composable
private fun ErrorBlock(message: String) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(12)) {
        Text("⚠️", style = TextStyle(fontWeight = FontWeight.Bold))
        Box(GlanceModifier.height(4)) {}
        Text(message, style = TextStyle(color = GlassColors.textSecondary), maxLines = 2)
    }
}

@Composable
private fun BalanceBlock(state: WidgetState) {
    val symbol = if (state.currency == "USD") "$" else "¥"

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Box(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "$symbol ${state.totalBalance}",
                style = TextStyle(color = GlassColors.textPrimary, fontWeight = FontWeight.Bold)
            )
        }
        Box(GlanceModifier.height(2)) {}
        Box(modifier = GlanceModifier.fillMaxWidth()) {
            Text("Balance", style = TextStyle(color = GlassColors.textTertiary))
        }
        Box(GlanceModifier.height(12)) {}
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Box(GlanceModifier.defaultWeight()) {
                BalanceChip("Granted", state.grantedBalance, symbol)
            }
            Box(GlanceModifier.width(12)) {}
            Box(GlanceModifier.defaultWeight()) {
                BalanceChip("Top-up", state.toppedUpBalance, symbol)
            }
        }
        if (state.lastUpdated > 0L) {
            Box(GlanceModifier.height(8)) {}
            Box(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = formatLastUpdated(state.lastUpdated),
                    style = TextStyle(color = GlassColors.textTertiary),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BalanceChip(label: String, amount: String, symbol: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlassColors.itemBackground)
            .cornerRadius(12)
            .padding(horizontal = 14, vertical = 6)
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "$symbol $amount",
                style = TextStyle(color = GlassColors.textPrimary, fontWeight = FontWeight.Medium)
            )
            Box(GlanceModifier.height(1)) {}
            Text(label, style = TextStyle(color = GlassColors.textTertiary))
        }
    }
}

@Composable
private fun BottomRow(state: WidgetState, onRefresh: Action) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(
            text = when {
                state.isLoading -> "⏳ Syncing"
                state.hasError && !state.hasData -> "⚠️ ${state.errorMessage ?: "Error"}"
                state.hasData && state.isAvailable -> "✅ Active"
                state.hasData && !state.isAvailable -> "❌ Inactive"
                else -> "⚙️ Setup"
            },
            style = TextStyle(
                color = when {
                    state.isLoading -> GlassColors.textTertiary
                    state.hasError && !state.hasData -> GlassColors.statusUnavailable
                    state.hasData && state.isAvailable -> GlassColors.statusAvailable
                    state.hasData && !state.isAvailable -> GlassColors.statusUnavailable
                    else -> GlassColors.textSecondary
                },
                fontWeight = if (state.hasData) FontWeight.Medium else FontWeight.Normal
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
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
