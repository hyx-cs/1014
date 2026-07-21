package com.example.deepseekwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
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
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(0x33000000.toInt()))
                        .padding(12)
                ) {
                    Text(
                        text = "Hello DeepSeek",
                        style = TextStyle(
                            color = ColorProvider(0xFFFFFFFF.toInt()),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Box(GlanceModifier.height(8)) {}
                    Text(
                        text = "If you see this, widget works",
                        style = TextStyle(color = ColorProvider(0x99FFFFFF.toInt()))
                    )
                }
            }
        }
    }
}
