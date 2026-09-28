package dirtyfrag.galaxy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dirtyfrag.galaxy.ui.theme.Purple
import dirtyfrag.galaxy.ui.theme.Surface2
import dirtyfrag.galaxy.ui.theme.TextPrimary
import dirtyfrag.galaxy.ui.theme.TextSecondary
import dirtyfrag.galaxy.ui.theme.Warning

/** Shown while the app is waiting for the user to grant ROOT in the KernelSU manager. */
@Composable
fun GrantBanner(
    visible: Boolean,
    onOpenManager: () -> Unit,
    onRecheck: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
        exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
    ) {
        Column(
            modifier
                .fillMaxWidth()
                .background(Surface2, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text("루트 권한이 필요합니다", style = MaterialTheme.typography.titleMedium, color = Warning)
            Text(
                "KernelSU 매니저 > 슈퍼유저 에서 이 앱(dirtyfrag.galaxy)의 ROOT 스위치를 켜세요.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenManager,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("매니저 열기", color = TextPrimary) }
                OutlinedButton(
                    onClick = onRecheck,
                    shape = RoundedCornerShape(14.dp)
                ) { Text("권한 다시 확인", color = TextPrimary) }
            }
        }
    }
}
