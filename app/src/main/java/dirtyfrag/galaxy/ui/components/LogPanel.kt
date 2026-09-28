package dirtyfrag.galaxy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dirtyfrag.galaxy.ui.theme.Rose
import dirtyfrag.galaxy.ui.theme.Surface2
import dirtyfrag.galaxy.ui.theme.Teal
import dirtyfrag.galaxy.ui.theme.TextPrimary
import dirtyfrag.galaxy.ui.theme.TextSecondary

val MonoStyle = androidx.compose.ui.text.TextStyle(
    fontFamily = FontFamily.Monospace, fontSize = 11.5.sp, lineHeight = 16.sp
)

/** Colour a log line by its severity prefix, matching the Specter palette. */
fun logLineColor(line: String) = when {
    line.startsWith("[!]") -> Rose
    line.startsWith("[+]") -> dirtyfrag.galaxy.ui.theme.Emerald
    line.startsWith("[*]") -> Teal
    else -> TextPrimary
}

@Composable
private fun LogLine(line: String, modifier: Modifier = Modifier) {
    var visible by remember(line) { mutableStateOf(false) }
    LaunchedEffect(line) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 3 },
        modifier = modifier
    ) {
        Text(line, style = MonoStyle, color = logLineColor(line), maxLines = 1)
    }
}

/** Compact 3-line preview shown on Home; tapping opens the full Log tab. */
@Composable
fun LogPreview(lines: List<String>, onOpenLog: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(16.dp))
            .clickable { onOpenLog() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LOG", style = MaterialTheme.typography.labelSmall, color = Teal)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("전체 보기", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = TextSecondary, modifier = Modifier.height(16.dp)
                )
            }
        }
        if (lines.isEmpty()) {
            Text("대기 중…", style = MonoStyle, color = TextSecondary)
        } else {
            Box(Modifier.padding(top = 4.dp)) {
                Column {
                    lines.takeLast(3).forEach { LogLine(it) }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Surface2.copy(alpha = 0f), Surface2.copy(alpha = 0.9f))
                            )
                        )
                )
            }
        }
    }
}

/** Full detailed log list used by the Log tab. */
@Composable
fun LogList(lines: List<String>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (lines.isEmpty()) {
            Text("로그가 없습니다", style = MonoStyle, color = TextSecondary)
        } else {
            lines.forEach { line ->
                Text(line, style = MonoStyle, color = logLineColor(line), modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}
