package dirtyfrag.galaxy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dirtyfrag.galaxy.ui.theme.StepBlue
import dirtyfrag.galaxy.ui.theme.StepCyan
import dirtyfrag.galaxy.ui.theme.StepGreen
import dirtyfrag.galaxy.ui.theme.StepOrange
import dirtyfrag.galaxy.ui.theme.StepRed
import dirtyfrag.galaxy.ui.theme.StepViolet
import dirtyfrag.galaxy.ui.theme.StepYellow
import dirtyfrag.galaxy.ui.theme.TextSecondary

private val Rainbow = listOf(StepRed, StepOrange, StepYellow, StepGreen, StepCyan, StepBlue, StepViolet)

/** "DirtyFrag" wordmark whose rainbow sweep flows D -> g, seamlessly looping. */
@Composable
fun RainbowTitle(text: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "rainbow")
    val phase by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "phase"
    )
    var w by remember { mutableStateOf(0f) }
    val span = if (w > 0f) w else 520f
    val doubled = remember { Rainbow + Rainbow }
    val x = -span + phase * span

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            onTextLayout = { w = it.size.width.toFloat() },
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = doubled,
                    start = Offset(x, 0f),
                    end = Offset(x + span * 2f, 0f)
                ),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.2.sp
            )
        )
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
