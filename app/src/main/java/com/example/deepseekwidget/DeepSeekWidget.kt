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
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

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

private val BG = ColorProvider(0x33000000.toInt())
private val WHITE = ColorProvider(0xFFFFFFFF.toInt())
private val GRAY = ColorProvider(0x99FFFFFF.toInt())
private val GREEN = ColorProvider(0xFF4CAF50.toInt())
private val RED = ColorProvider(0xFFF44336.toInt())
private val AMBER = ColorProvider(0xFFFFC107.toInt())

@Composable
private fun WidgetContent(state: WidgetState, onRefresh: Action) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(BG)
            .padding(12)
    ) {
        // 标题
        Text(
            text = "DeepSeek API",
            style = TextStyle(color = WHITE, fontWeight = FontWeight.Bold)
        )
        Box(GlanceModifier.height(8)) {}

        // 内容
        if (state.isLoading) {
            Text("Loading...", style = TextStyle(color = AMBER))
        } else if (state.hasError && !state.hasData) {
            Text(
                text = state.errorMessage ?: "Error",
                style = TextStyle(color = RED)
            )
        } else {
            val symbol = if (state.currency == "USD") "$" else "¥"
            Text(
                text = "$symbol ${state.totalBalance}",
                style = TextStyle(color = WHITE, fontWeight = FontWeight.Bold)
            )
            Box(GlanceModifier.height(4)) {}
            Text(
                text = "Granted: $symbol ${state.grantedBalance}  |  Top-up: $symbol ${state.toppedUpBalance}",
                style = TextStyle(color = GRAY)
            )
        }

        Box(GlanceModifier.height(8)) {}

        // 底部
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = if (state.hasData && state.isAvailable) "Active"
                else if (state.hasData) "Inactive"
                else if (state.isLoading) "Syncing"
                else "Setup",
                style = TextStyle(
                    color = when {
                        state.isLoading -> AMBER
                        state.hasData && state.isAvailable -> GREEN
                        state.hasData -> RED
                        else -> GRAY
                    }
                )
            )
            Box(GlanceModifier.width(12)) {}
            Text(
                text = "Refresh",
                modifier = GlanceModifier.clickable(onRefresh),
                style = TextStyle(color = GRAY)
            )
        }
    }
}

private val WidgetState.hasData get() = totalBalance.isNotEmpty()
private val WidgetState.hasError get() = errorMessage != null
