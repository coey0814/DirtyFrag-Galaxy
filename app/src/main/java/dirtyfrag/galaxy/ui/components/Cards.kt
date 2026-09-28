package dirtyfrag.galaxy.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dirtyfrag.galaxy.core.HealthStatus
import dirtyfrag.galaxy.core.ModuleState
import dirtyfrag.galaxy.core.ModuleStatus
import dirtyfrag.galaxy.ui.theme.Emerald
import dirtyfrag.galaxy.ui.theme.Rose
import dirtyfrag.galaxy.ui.theme.Surface2
import dirtyfrag.galaxy.ui.theme.TextPrimary
import dirtyfrag.galaxy.ui.theme.TextSecondary

private fun stateColor(state: ModuleState): Color = when (state) {
    ModuleState.INSTALLED -> Emerald
    ModuleState.FAILED -> Rose
    ModuleState.NOT_INSTALLED -> TextSecondary
}

private fun stateLabel(state: ModuleState): String = when (state) {
    ModuleState.INSTALLED -> "installed"
    ModuleState.FAILED -> "missing"
    ModuleState.NOT_INSTALLED -> "not installed"
}

@Composable
fun ModuleCard(status: ModuleStatus, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val color = stateColor(status.state)
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(Surface2, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .animateContentSize(tween(200))
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(status.kind.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
                Text("  ${stateLabel(status.state)}", style = MaterialTheme.typography.labelSmall, color = color)
            }
        }
        if (status.detail.isNotBlank()) {
            Text(status.detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun HealthRow(item: HealthStatus, modifier: Modifier = Modifier) {
    val color = if (item.ok) Emerald else Rose.copy(alpha = 0.85f)
    Row(
        modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(color, RoundedCornerShape(4.dp)))
        Text(
            "  ${item.health.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            item.detail,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
