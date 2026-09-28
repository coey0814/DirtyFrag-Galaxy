package dirtyfrag.galaxy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import dirtyfrag.galaxy.core.StepState
import dirtyfrag.galaxy.core.StepStatus
import dirtyfrag.galaxy.ui.theme.Outline
import dirtyfrag.galaxy.ui.theme.Rose
import dirtyfrag.galaxy.ui.theme.TextPrimary
import dirtyfrag.galaxy.ui.theme.TextSecondary
import dirtyfrag.galaxy.ui.theme.stepColor

/** Vertical root-flow checklist with rainbow per-step colours and spring check marks. */
@Composable
fun StepTracker(steps: List<StepStatus>, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 4.dp)) {
        steps.forEachIndexed { index, s ->
            val color = stepColor(s.step)
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StepDot(s.state, color)
                    if (index != steps.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(26.dp)
                                .background(
                                    if (s.state == StepState.DONE) color.copy(alpha = 0.55f)
                                    else Outline
                                )
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.padding(top = 2.dp, bottom = 6.dp)) {
                    Text(
                        s.step.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (s.state) {
                            StepState.DONE -> lerp(color, Color.White, 0.4f)
                            StepState.ACTIVE -> color
                            StepState.FAILED -> Rose
                            StepState.PENDING -> TextSecondary
                        }
                    )
                    if (s.detail.isNotBlank()) {
                        Text(s.detail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDot(state: StepState, color: Color) {
    val bg = when (state) {
        StepState.DONE -> color
        StepState.ACTIVE -> color.copy(alpha = 0.22f)
        StepState.FAILED -> Rose.copy(alpha = 0.18f)
        StepState.PENDING -> Color.Transparent
    }
    val border = when (state) {
        StepState.DONE -> color
        StepState.ACTIVE -> color
        StepState.FAILED -> Rose
        StepState.PENDING -> Outline
    }
    val scale by animateFloatAsState(
        targetValue = if (state == StepState.DONE) 1f else 0.9f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "dot"
    )

    Box(
        Modifier
            .size(26.dp)
            .scale(scale)
            .background(bg, CircleShape)
            .border(1.5.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            StepState.DONE -> AnimatedVisibility(
                visible = true,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
            ) {
                Icon(Icons.Filled.Check, null, tint = Color(0xFF0B1020), modifier = Modifier.size(16.dp))
            }
            StepState.ACTIVE -> CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = color
            )
            StepState.FAILED -> Icon(Icons.Filled.Close, null, tint = Rose, modifier = Modifier.size(15.dp))
            StepState.PENDING -> Box(Modifier.size(6.dp).background(Outline, CircleShape))
        }
    }
}
