package dirtyfrag.galaxy.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dirtyfrag.galaxy.ui.theme.Success
import dirtyfrag.galaxy.ui.theme.Surface2
import dirtyfrag.galaxy.ui.theme.TextPrimary
import dirtyfrag.galaxy.ui.theme.TextSecondary
import dirtyfrag.galaxy.ui.theme.Warning

fun hasOverlayPermission(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

fun openOverlaySettings(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Overlay-permission state that refreshes when the app returns to the foreground. */
@Composable
fun rememberOverlayGranted(): Boolean {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(hasOverlayPermission(ctx)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasOverlayPermission(ctx)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return granted
}

@Composable
fun OverlayPermissionDialog(onAllow: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        title = { Text("권한 필요 · 다른 앱 위에 표시", color = TextPrimary) },
        text = {
            Text(
                "부팅 시 자동 루팅을 쓰려면 이 권한이 필요합니다.\n\n" +
                    "콜드 부팅 직후 su 바이너리는 KernelSU 매니저가 실행될 때 설치됩니다. " +
                    "백그라운드 서비스는 이 권한이 있어야 매니저를 열 수 있습니다.\n\n" +
                    "설정 > 특수 접근 > 다른 앱 위에 표시 에서 이 앱을 허용하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = { Button(onClick = onAllow, shape = RoundedCornerShape(12.dp)) { Text("권한 허용") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("나중에", color = TextSecondary) } }
    )
}

/** Settings row that reflects + requests the overlay permission. */
@Composable
fun OverlayPermissionCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val granted = rememberOverlayGranted()
    Column(modifier.fillMaxWidth()) {
        Text("권한", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Text("다른 앱 위에 표시 (부팅 자동 루팅 필수)", color = TextPrimary)
            Text(
                if (granted) "✓ 허용됨" else "필요 — 자동 루팅 시 KernelSU 매니저를 열기 위해 필요",
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) Success else Warning
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { openOverlaySettings(ctx) },
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (granted) "설정 열기" else "권한 허용하기") }
        }
    }
}
