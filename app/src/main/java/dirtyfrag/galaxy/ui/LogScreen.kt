package dirtyfrag.galaxy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dirtyfrag.galaxy.core.LogBus
import dirtyfrag.galaxy.core.RootStage
import dirtyfrag.galaxy.core.RootViewModel
import dirtyfrag.galaxy.ui.components.GrantBanner
import dirtyfrag.galaxy.ui.components.LogList
import dirtyfrag.galaxy.ui.theme.Surface1
import dirtyfrag.galaxy.ui.theme.Teal
import dirtyfrag.galaxy.ui.theme.TextSecondary

@Composable
fun LogScreen(vm: RootViewModel, modifier: Modifier = Modifier) {
    val lines by LogBus.lines.collectAsStateWithLifecycle()
    val steps by vm.steps.collectAsStateWithLifecycle()
    val stage by vm.stage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scroll = rememberScrollState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) scroll.animateScrollTo(scroll.maxValue)
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LOG", style = MaterialTheme.typography.headlineMedium, color = Teal)
            Row {
                IconButton(onClick = { copyLog(context) }) {
                    Icon(Icons.Filled.ContentCopy, "copy", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { shareLog(context) }) {
                    Icon(Icons.Filled.Share, "share", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { LogBus.clear() }) {
                    Icon(Icons.Filled.DeleteOutline, "clear", tint = TextSecondary, modifier = Modifier.size(22.dp))
                }
            }
        }

        GrantBanner(
            visible = stage == RootStage.S3_AWAITING_ROOT_GRANT,
            onOpenManager = { vm.openManager() },
            onRecheck = { vm.recheckGrant() },
            modifier = Modifier.padding(top = 10.dp)
        )

        Column(
            Modifier.fillMaxWidth().padding(top = 8.dp).background(Surface1, RoundedCornerShape(16.dp)).padding(14.dp)
        ) {
            Text("진행 단계", style = MaterialTheme.typography.labelSmall, color = Teal)
            dirtyfrag.galaxy.ui.components.StepTracker(steps)
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp).verticalScroll(scroll)
        ) {
            LogList(lines)
        }
    }
}

private fun copyLog(context: Context) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("dirtyfrag log", LogBus.text()))
    Toast.makeText(context, "로그 복사됨", Toast.LENGTH_SHORT).show()
}

private fun shareLog(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, LogBus.text())
    }
    context.startActivity(Intent.createChooser(send, "로그 공유"))
}
