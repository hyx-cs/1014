package com.example.deepseekwidget.ui

import androidx.glance.unit.ColorProvider

/**
 * 毛玻璃小组件配色常量。
 *
 * 所有颜色使用 ARGB Int 值。
 * Glance 使用 [ColorProvider] 而非 Compose 的 [Color]，
 * 因此集中定义在此处，保持一致性和可维护性。
 *
 * 设计原则：
 * - 白色系文字，通过不同透明度表达层级
 * - 状态色保持可辨识度（在浅色和深色壁纸上均可见）
 * - 整体呈现 iOS 毛玻璃的轻盈通透感
 */
object GlassColors {

    // ── 文字层级 ──────────────────────────────

    /** 主文字 90% 白 —— 大号余额数字、标题 */
    val textPrimary = ColorProvider(0xE6FFFFFF.toInt())

    /** 副文字 70% 白 —— 标签、细分金额 */
    val textSecondary = ColorProvider(0xB3FFFFFF.toInt())

    /** 辅助文字 50% 白 —— 单位、次要标注 */
    val textTertiary = ColorProvider(0x80FFFFFF.toInt())

    // ── 状态指示 ──────────────────────────────

    /** 可用状态 — 柔和绿 */
    val statusAvailable = ColorProvider(0xE64CAF50.toInt())

    /** 不可用状态 — 柔和红 */
    val statusUnavailable = ColorProvider(0xE6F44336.toInt())

    /** 加载中 — 琥珀 */
    val statusLoading = ColorProvider(0xE6FFC107.toInt())

    // ── 背景装饰 ──────────────────────────────

    /** 细分 item 背景 — 极淡白 */
    val itemBackground = ColorProvider(0x1AFFFFFF.toInt())
}
