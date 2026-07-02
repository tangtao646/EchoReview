package com.dream.echoreview.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 15
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            // 计算每个条条的基础高度偏移，制造一种流动的效果
            val randomFactor = remember { Random.nextFloat() * 0.4f + 0.6f }
            val animatedHeight by animateFloatAsState(
                targetValue = (amplitude * 120.dp.value * randomFactor).coerceAtLeast(8.dp.value),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "barHeight"
            )

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (amplitude > 0.1f) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            )
        }
    }
}
