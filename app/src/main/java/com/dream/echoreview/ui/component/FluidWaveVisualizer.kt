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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun FluidWaveVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 50
    val barWidth = 3.dp
    val barSpacing = 2.dp
    
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "amplitude"
    )

    val color1 = Color(0xFF64B5F6) // Light Blue
    val color2 = Color(0xFF81C784) // Light Green
    val idleColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val totalBarWidth = barWidth.toPx()
        val spacing = barSpacing.toPx()
        val startX = (width - (barCount * (totalBarWidth + spacing))) / 2

        for (i in 0 until barCount) {
            // 结合正弦波与实时振幅，创造流动感
            val sinWave = sin(i.toFloat() * 0.2f + phase) * 0.2f + 0.8f
            val variation = when {
                i < 10 -> i / 10f
                i > barCount - 10 -> (barCount - i) / 10f
                else -> 1f
            }
            
            val minHeight = 4.dp.toPx()
            val maxHeight = height * 0.8f
            val currentBarHeight = (animatedAmplitude * maxHeight * variation * sinWave).coerceAtLeast(minHeight)

            val x = startX + i * (totalBarWidth + spacing)
            val y = centerY - (currentBarHeight / 2)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color1, color2),
                    startY = y,
                    endY = y + currentBarHeight
                ),
                topLeft = Offset(x, y),
                size = Size(totalBarWidth, currentBarHeight),
                cornerRadius = CornerRadius(totalBarWidth / 2)
            )
            
            // 绘制一个半透明的叠加层，增加深度感
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(x, y),
                size = Size(totalBarWidth, currentBarHeight / 2),
                cornerRadius = CornerRadius(totalBarWidth / 2)
            )
        }
    }
}
