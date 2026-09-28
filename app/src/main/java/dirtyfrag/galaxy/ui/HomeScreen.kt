package dirtyfrag.galaxy.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dirtyfrag.galaxy.core.HealthStatus
import dirtyfrag.galaxy.core.LogBus
import dirtyfrag.galaxy.core.ModuleKind
import dirtyfrag.galaxy.core.ModuleState
import dirtyfrag.galaxy.core.ModuleStatus
import dirtyfrag.galaxy.core.RootStage
import dirtyfrag.galaxy.core.RootViewModel
import dirtyfrag.galaxy.core.StepStatus
import dirtyfrag.galaxy.ui.components.GrantBanner
import dirtyfrag.galaxy.ui.components.HealthRow
import dirtyfrag.galaxy.ui.components.LogPreview
import dirtyfrag.galaxy.ui.components.ModuleCard
import dirtyfrag.galaxy.ui.components.RainbowTitle
import dirtyfrag.galaxy.ui.components.SectionTitle
import dirtyfrag.galaxy.ui.components.StateOrb
import dirtyfrag.galaxy.ui.components.StepTracker
import dirtyfrag.galaxy.ui.theme.AuroraButton
import dirtyfrag.galaxy.ui.theme.Dur
import dirtyfrag.galaxy.ui.theme.SpecterEase
import dirtyfrag.galaxy.ui.theme.Surface1
import dirtyfrag.galaxy.ui.theme.TextPrimary
import dirtyfrag.galaxy.ui.theme.TextSecondary
import dirtyfrag.galaxy.ui.theme.stageColor
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    vm: RootViewModel,
    onOpenLog: () -> Unit,
    onRootStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage by vm.stage.collectAsStateWithLifecycle()
    val steps by vm.steps.collectAsStateWithLifecycle()
    val modules by vm.modules.collectAsStateWithLifecycle()
    val health by vm.health.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val lines by LogBus.lines.collectAsStateWithLifecycle()

    val landscape = LocalConfiguration.current.screenWidthDp > LocalConfiguration.current.screenHeightDp

    if (landscape) {
        Row(
            modifier.fillMaxSize().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Entrance(0) { RainbowTitle("DirtyFrag Galaxy", "임시 루팅 & 모듈 오케스트레이터") }
                Spacer(Modifier.height(2.dp))
                Box(
                    Modifier.fillMaxWidth().height(170.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Entrance(1) { StateOrb(stage, size = 122.dp) }
                }
                Entrance(2) { StageTitle(stage) }
                Spacer(Modifier.height(4.dp))
                Entrance(3) { ActionButtons(vm, stage, running, onRootStart) }
                GrantBanner(
                    visible = stage == RootStage.S3_AWAITING_ROOT_GRANT,
                    onOpenManager = { vm.openManager() },
                    onRecheck = { vm.recheckGrant() },
                    modifier = Modifier.padding(top = 12.dp)
                )
                ModulesGuide(stage, modules) { vm.openManager() }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Entrance(0) { StepsSection(steps) }
                Entrance(1) { HealthSection(health) }
                Entrance(2) { ModulesSection(modules) }
                Entrance(3) { Column(Modifier.padding(top = 8.dp)) { LogPreview(lines, onOpenLog) } }
                Spacer(Modifier.height(24.dp))
            }
        }
    } else {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Entrance(0) { RainbowTitle("DirtyFrag", "임시 루팅 & 모듈 오케스트레이터") }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().height(246.dp),
                contentAlignment = Alignment.Center
            ) {
                Entrance(1) { StateOrb(stage) }
            }
            Entrance(2) { StageTitle(stage) }
            Spacer(Modifier.height(14.dp))
            Entrance(3) { ActionButtons(vm, stage, running, onRootStart) }
            GrantBanner(
                visible = stage == RootStage.S3_AWAITING_ROOT_GRANT,
                onOpenManager = { vm.openManager() },
                onRecheck = { vm.recheckGrant() },
                modifier = Modifier.padding(top = 12.dp)
            )
            Spacer(Modifier.height(6.dp))
            Entrance(4) { StepsSection(steps) }
            Entrance(5) { HealthSection(health) }
            Entrance(6) { ModulesSection(modules) }
            Entrance(7) { Column(Modifier.padding(top = 8.dp)) { LogPreview(lines, onOpenLog) } }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Entrance(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 55L)
        shown = true
    }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(320, easing = SpecterEase)) + slideInVertically(tween(320, easing = SpecterEase)) { it / 6 },
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun StageTitle(stage: RootStage) {
    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            (fadeIn(tween(Dur.BASE, easing = SpecterEase)) +
                slideInVertically(tween(Dur.BASE, easing = SpecterEase)) { it / 4 })
                .togetherWith(
                    fadeOut(tween(Dur.FAST)) +
                        slideOutVertically(tween(Dur.FAST)) { -it / 4 }
                )
        },
        label = "stage"
    ) { s ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stageName(s),
                style = MaterialTheme.typography.headlineMedium,
                color = stageColor(s),
                fontWeight = FontWeight.SemiBold
            )
            Text(stageCaption(s), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun StepsSection(steps: List<StepStatus>) {
    Column(Modifier.fillMaxWidth()) {
        SectionTitle("진행 단계")
        StepTracker(steps)
    }
}

@Composable
private fun HealthSection(health: List<HealthStatus>) {
    Column(Modifier.fillMaxWidth()) {
        SectionTitle("헬스")
        health.forEach { HealthRow(it) }
    }
}

@Composable
private fun ModulesSection(modules: List<ModuleStatus>) {
    if (modules.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        SectionTitle("모듈 감지")
        modules.forEach { status ->
            ModuleCard(status, Modifier.padding(vertical = 4.dp), onClick = null)
        }
    }
}

/**
 * First-run guidance: root is already acquired, but modules cannot be installed
 * until after root, so we point the user at KernelSU manager > Modules and let
 * them resume by pressing ROOT again.
 */
@Composable
private fun ModulesGuide(stage: RootStage, modules: List<ModuleStatus>, onOpenManager: () -> Unit) {
    if (stage != RootStage.S2_ROOTED_MODULES_MISSING) return
    val missing = modules.filter { it.state != ModuleState.INSTALLED }
    val managerMissing = missing.any { it.kind == ModuleKind.MANAGER }
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(Surface1, RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Text("루트 획득 완료 — 모듈 설치 필요", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        if (managerMissing) {
            Text(
                "KernelSU 매니저 앱을 먼저 설치한 뒤, 이 앱에서 다시 [루팅]을 누르세요.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        } else {
            Text(
                "첫 사용자는 지금부터 KernelSU 매니저 > 모듈 에서 Zygisk-Next 와 LSPosed(zip)를 설치하세요. " +
                    "설치 후 이 화면에서 다시 [루팅]을 누르면 자동으로 계속됩니다.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        }
        Spacer(Modifier.height(8.dp))
        missing.forEach { m ->
            Text("•  ${m.kind.label}", style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                modifier = Modifier.padding(vertical = 1.dp))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onOpenManager, shape = RoundedCornerShape(14.dp)) {
            Text("KernelSU 매니저 열기", color = TextPrimary)
        }
    }
}

@Composable
private fun ActionButtons(vm: RootViewModel, stage: RootStage, running: Boolean, onRootStart: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressButton(
            text = when {
                running -> "취소"
                stage == RootStage.S7_ACTIVE -> "재루팅 / 검증"
                else -> "루팅"
            },
            primary = true,
            enabled = true,
            onClick = {
                if (running) {
                    vm.cancel()
                } else {
                    vm.oneClick()
                    onRootStart()
                }
            }
        )
        PressButton(
            text = "소프트 재시작",
            primary = false,
            enabled = !running && File("/dev/df").exists(),
            onClick = { vm.softRestart() }
        )
    }
}

@Composable
private fun PressButton(text: String, primary: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120, easing = SpecterEase), label = "press")

    if (primary) {
        Box(
            Modifier
                .scale(scale)
                .background(
                    if (enabled) AuroraButton
                    else Brush.horizontalGradient(
                        listOf(TextSecondary.copy(alpha = 0.35f), TextSecondary.copy(alpha = 0.35f))
                    ),
                    RoundedCornerShape(18.dp)
                )
                .clickable(enabled = enabled, interactionSource = interaction, indication = null) { onClick() }
                .padding(horizontal = 30.dp, vertical = 13.dp)
        ) {
            Text(text, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            interactionSource = interaction,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.scale(scale)
        ) { Text(text, color = TextPrimary) }
    }
}

private fun stageName(stage: RootStage): String = when (stage) {
    RootStage.S0_UNROOTED -> "Root & Modules"
    RootStage.S1_ROOTING -> "Rooting…"
    RootStage.S2_ROOTED_MODULES_MISSING -> "Rooted!"
    RootStage.S3_AWAITING_ROOT_GRANT -> "Root Access Required"
    RootStage.S4_INSTALLING -> "Checking Modules"
    RootStage.S5_VERIFYING -> "Verifying…"
    RootStage.S6_READY_SOFT_RESTART -> "Ready"
    RootStage.S7_ACTIVE -> "Fully Activated"
}

private fun stageCaption(stage: RootStage): String = when (stage) {
    RootStage.S0_UNROOTED -> "원클릭 임시 루팅"
    RootStage.S1_ROOTING -> "익스플로잇 진행 중"
    RootStage.S2_ROOTED_MODULES_MISSING -> "KernelSU 매니저에서 모듈 설치 후 다시 루팅"
    RootStage.S3_AWAITING_ROOT_GRANT -> "매니저에서 이 앱의 ROOT를 허용하세요"
    RootStage.S4_INSTALLING -> "권장 모듈 감지 중"
    RootStage.S5_VERIFYING -> "root / zygisk / lsposed 검증 중"
    RootStage.S6_READY_SOFT_RESTART -> "soft restart로 모듈 활성화"
    RootStage.S7_ACTIVE -> "root + modules + hooks"
}
