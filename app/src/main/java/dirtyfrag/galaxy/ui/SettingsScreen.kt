package dirtyfrag.galaxy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dirtyfrag.galaxy.core.ModuleState
import dirtyfrag.galaxy.core.RootViewModel
import dirtyfrag.galaxy.ui.components.AutoToggle
import dirtyfrag.galaxy.ui.components.OverlayPermissionCard
import dirtyfrag.galaxy.ui.components.SectionTitle
import dirtyfrag.galaxy.ui.theme.Emerald
import dirtyfrag.galaxy.ui.theme.Surface1
import dirtyfrag.galaxy.ui.theme.Teal
import dirtyfrag.galaxy.ui.theme.TextPrimary
import dirtyfrag.galaxy.ui.theme.TextSecondary

@Composable
fun SettingsScreen(vm: RootViewModel, modifier: Modifier = Modifier) {
    val options by vm.options.collectAsStateWithLifecycle()
    val device by vm.device.collectAsStateWithLifecycle()
    val modules by vm.modules.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("설정", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

        SectionTitle("자동화")
        AutoToggle(
            title = "부팅 시 자동 루팅",
            subtitle = "재부팅 후 임시 루트를 자동 재적용",
            checked = options.autoRootOnBoot,
            onCheckedChange = { vm.setAutoRoot(it) }
        )
        AutoToggle(
            title = "자동 소프트 재시작",
            subtitle = "검증 후 자동으로 soft restart (기본 꺼짐)",
            checked = options.autoSoftRestart,
            onCheckedChange = { vm.setAutoSoftRestart(it) }
        )
        AutoToggle(
            title = "완료 후 SELinux Enforcing 복원",
            subtitle = "루트·Zygisk·LSPosed 확인 후 재부팅 없이 enforcing으로 전환",
            checked = options.restoreEnforcing,
            onCheckedChange = { vm.setRestoreEnforcing(it) }
        )

        Spacer(Modifier.height(14.dp))
        OverlayPermissionCard()

        SectionTitle("권장 모듈")
        Card {
            modules.forEach { m ->
                Text(
                    "${if (m.state == ModuleState.INSTALLED) "✓" else "•"}  ${m.kind.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (m.state == ModuleState.INSTALLED) Emerald else TextSecondary,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "KernelSU · Zygisk-Next · LSPosed 조합을 권장합니다. 이 앱은 다운로드/설치를 하지 않고 설치 여부만 감지합니다.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { vm.refreshLsposedManager() },
                shape = RoundedCornerShape(14.dp)
            ) { Text("LSPosed 매니저 새로고침", color = TextPrimary) }
        }

        SectionTitle("정보")
        Card {
            Text(
                "DirtyFrag Galaxy — DirtyFrag (CVE-2026-43284) 임시 루팅 + 모듈 오케스트레이터.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text("Credits", style = MaterialTheme.typography.labelSmall, color = Teal)
            Text(
                "LSPosed Team — lspromise (DirtyFrag PoC)",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                "diabl0w — DFRoot",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Text(
                "polygraphene — DFReroot",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Text(
                "combeng6th — DirtyInit",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
            Text(
                "KernelSU · Zygisk Next · LSPosed",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        }
        SectionTitle("기기")
        Card {
            InfoRow("모델", device.model)
            InfoRow("펌웨어", device.firmware)
            InfoRow("Android", device.androidRelease)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(16.dp)).padding(14.dp)) { content() }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        "$label  ·  $value",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
