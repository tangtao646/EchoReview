package com.dream.echoreview.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun FluidWaveVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 40
    val barWidth = 4.dp
    val barSpacing = 3.dp
    
    // 使用平滑处理的振幅，防止跳动过于生硬
    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "amplitude"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val totalBarWidth = barWidth.toPx()
        val spacing = barSpacing.toPx()
        val startX = (width - (barCount * (totalBarWidth + spacing))) / 2

        for (i in 0 until barCount) {
            // 为每个条柱计算不同的灵敏度，模拟波形的起伏感
            val variation = when {
                i < 10 -> i / 10f
                i > 30 -> (barCount - i) / 10f
                else -> 1f
            }
            
            // 基础高度：静音时为 2dp 的线
            val minHeight = 3.dp.toPx()
            val maxHeight = height * 0.9f
            val currentBarHeight = (animatedAmplitude * maxHeight * variation).coerceAtLeast(minHeight)

            val x = startX + i * (totalBarWidth + spacing)
            val y = centerY - (currentBarHeight / 2)

            drawRoundRect(
                color = if (amplitude > 0.05f) primaryColor else idleColor,
                topLeft = Offset(x, y),
                size = Size(totalBarWidth, currentBarHeight),
                cornerRadius = CornerRadius(totalBarWidth / 2)
            )
        }
    }
}
