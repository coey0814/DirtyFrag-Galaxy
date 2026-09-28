package dirtyfrag.galaxy.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dirtyfrag.galaxy.exploit.PostRoot
import dirtyfrag.galaxy.exploit.Reporter
import dirtyfrag.galaxy.exploit.RootExploit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RootViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = Settings(app)
    private val verifier = Verifier(app)

    private val _stage = MutableStateFlow(RootStage.S0_UNROOTED)
    val stage = _stage.asStateFlow()

    private val _health = MutableStateFlow<List<HealthStatus>>(emptyList())
    val health = _health.asStateFlow()

    private val _modules = MutableStateFlow(emptyList<ModuleStatus>())
    val modules = _modules.asStateFlow()

    private val _steps = MutableStateFlow(pendingSteps())
    val steps = _steps.asStateFlow()

    private val _options = MutableStateFlow(settings.options())
    val options = _options.asStateFlow()

    private val _device = MutableStateFlow(DeviceInfo())
    val device = _device.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()

    @Volatile
    private var suGranted = false

    @Volatile
    private var rootPresent = false

    @Volatile
    private var cancelled = false

    @Volatile
    private var autoActivateDone = false

    @Volatile
    private var lastLoggedStage: RootStage? = null

    @Volatile
    private var settleScheduled = false

    @Volatile
    private var settleDone = false

    @Volatile
    private var enforceRestoreDone = false

    @Volatile
    private var grantNotified = false

    @Volatile
    private var restartNotified = false

    private val MAX_ACTIVATION_ATTEMPTS = 3

    private val reporter = Reporter { msg -> LogBus.log(msg) }

    private val canceller = object : PostRoot.Canceller {
        override fun isCancelled(): Boolean = cancelled
    }

    init {
        LogBus.log("[*] DirtyFrag Galaxy 준비")
        syncBootReceiver()
        refresh()
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) { detect() }

    private suspend fun detect() {
        _device.value = verifier.device()
        rootPresent = verifier.rootPresent()
        suGranted = verifier.suGranted()
        val mods = verifier.moduleStatuses()
        _modules.value = mods
        val h = verifier.healthAll()
        _health.value = h
        // SELinux is informational only (see Verifier.selinux()): never part of the gate.
        val healthOk = h.filter { it.health != Health.HOOK && it.health != Health.PERMISSIVE }.all { it.ok }
        val modulesOk = mods.all { it.state == ModuleState.INSTALLED }
        val missingNames = mods.filter { it.state != ModuleState.INSTALLED }.joinToString(", ") { it.kind.label }
        val lsposedUp = h.firstOrNull { it.health == Health.LSPOSED }?.ok == true

        _stage.value = when {
            !rootPresent -> RootStage.S0_UNROOTED
            !suGranted -> RootStage.S3_AWAITING_ROOT_GRANT
            !modulesOk -> RootStage.S2_ROOTED_MODULES_MISSING
            healthOk && lsposedUp -> RootStage.S7_ACTIVE
            else -> RootStage.S6_READY_SOFT_RESTART
        }
        _steps.value = rootSteps(_stage.value, suGranted, modulesOk, rootPresent, h)

        // Detailed status snapshot in the log whenever the state changes while rooted.
        if (suGranted && _stage.value != lastLoggedStage) {
            lastLoggedStage = _stage.value
            LogBus.log("[*] ── 현재 상태 ──")
            h.forEach { LogBus.log("[${if (it.ok) "+" else "!"}] ${it.health.label}: ${it.detail}") }
            mods.forEach { LogBus.log("[${if (it.state == ModuleState.INSTALLED) "+" else "!"}] 모듈·${it.kind.label}: ${it.detail}") }
        }

        // Surface the grant request as a notification, but only alert ONCE per episode
        // (no popup available). Re-alerting on every detect was annoying.
        if (suGranted || !rootPresent) {
            grantNotified = false
            Notifier.cancelGrant(getApplication())
        } else if (_stage.value == RootStage.S3_AWAITING_ROOT_GRANT && !grantNotified) {
            grantNotified = true
            Notifier.grantRequest(getApplication())
        }

        // Module-install guidance notification: shown whenever we are rooted but the
        // recommended modules are still missing (first-run flow), independent of
        // whether the user pressed ROOT.
        if (modulesOk) {
            Notifier.cancelModule(getApplication())
        } else if (_stage.value == RootStage.S2_ROOTED_MODULES_MISSING) {
            Notifier.moduleInstallRequired(getApplication(), missingNames)
        }

        // Soft-restart-needed guidance: root + modules are ready but LSPosed is not yet
        // injected (needs a framework restart). The UI just spun without telling the user.
        if (_stage.value == RootStage.S6_READY_SOFT_RESTART && suGranted && modulesOk) {
            if (!restartNotified) {
                restartNotified = true
                LogBus.log("[*] LSPosed 정상 작동을 위해 소프트 재시작이 필요합니다 — [소프트 재시작]을 누르세요.")
                Notifier.softRestartNeeded(getApplication())
            }
        } else {
            restartNotified = false
            Notifier.cancelSoftRestart(getApplication())
        }

        // Auto-activate: on a fresh root the first soft reboot does not always get
        // LSPosed into system_server, so retry (matching the manual 3x workflow).
        val st = _stage.value
        if (st == RootStage.S7_ACTIVE) {
            if (settings.activationAttempts != 0) settings.activationAttempts = 0
            settleDone = false
            // Root + Zygisk + LSPosed confirmed: restore SELinux Enforcing in place,
            // WITHOUT a reboot (the sticky-permissive KO is told to stop re-asserting).
            if (settings.restoreEnforcing && !enforceRestoreDone && !cancelled && suGranted) {
                enforceRestoreDone = true
                viewModelScope.launch(Dispatchers.IO) {
                    LogBus.log("[*] SELinux Enforcing 복원 시도 (재부팅 없이)")
                    val ok = PostRoot.restoreEnforcing(reporter, canceller)
                    LogBus.log(if (ok) "[+] SELinux: Enforcing" else "[!] SELinux 복원 실패 — 현재 상태 유지")
                    detect()
                }
            }
        } else if (!autoActivateDone && !cancelled && settings.autoSoftRestart &&
            st == RootStage.S6_READY_SOFT_RESTART && suGranted && rootPresent && modulesOk &&
            settings.activationAttempts < MAX_ACTIVATION_ATTEMPTS
        ) {
            if (!settleDone && !settleScheduled) {
                // Give the framework a moment to finish injecting system_server before
                // deciding to soft-reboot again (avoids a false "미주입" + reboot loop).
                settleScheduled = true
                LogBus.log("[*] LSPosed 주입 대기 중… (12초 후 재확인)")
                viewModelScope.launch(Dispatchers.IO) {
                    kotlinx.coroutines.delay(12_000)
                    settleScheduled = false
                    settleDone = true
                    detect()
                }
            } else if (settleDone) {
                autoActivateDone = true
                settings.activationAttempts++
                LogBus.log("[*] LSPosed 미활성 — 자동 소프트 재시작 재시도 (${settings.activationAttempts}/$MAX_ACTIVATION_ATTEMPTS)")
                viewModelScope.launch(Dispatchers.IO) { PostRoot.softReboot(reporter, canceller) }
            }
        }
    }

    fun setAutoRoot(v: Boolean) {
        settings.autoRootOnBoot = v
        _options.value = settings.options()
        syncBootReceiver()
    }

    fun setAutoSoftRestart(v: Boolean) {
        settings.autoSoftRestart = v
        _options.value = settings.options()
        syncBootReceiver()
    }

    fun setRestoreEnforcing(v: Boolean) {
        settings.restoreEnforcing = v
        _options.value = settings.options()
        if (!v) enforceRestoreDone = false
    }

    /** Enable the manifest BootReceiver only when boot auto-root is on. */
    private fun syncBootReceiver() {
        setBootReceiverEnabled(getApplication(), settings.autoRootOnBoot)
    }

    companion object {
        fun setBootReceiverEnabled(ctx: android.content.Context, enabled: Boolean) {
            val comp = android.content.ComponentName(ctx, dirtyfrag.galaxy.exploit.BootReceiver::class.java)
            ctx.packageManager.setComponentEnabledSetting(
                comp,
                if (enabled) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        }
    }

    fun cancel() {
        cancelled = true
        LogBus.log("[*] 취소 요청 — 진행 중 작업 중단")
    }

    fun clearLog() = LogBus.clear()

    /** Open the KernelSU manager so the user can toggle ROOT for this app. */
    fun openManager() {
        PostRoot.launchManager(getApplication(), KERNELSU_PKG, reporter)
    }

    /**
     * Force-stop + relaunch the LSPosed manager so it re-forks as a fresh,
     * Zygisk-injected zygote child. Fixes the late-load "설치되지 않음" state.
     */
    fun refreshLsposedManager() = viewModelScope.launch(Dispatchers.IO) {
        LogBus.log("[*] LSPosed 매니저 새로고침 (강제종료 후 재실행)")
        SysUtil.su("am force-stop $LSPOSED_MANAGER_PKG")
        val i = android.content.Intent().apply {
            component = android.content.ComponentName(
                LSPOSED_MANAGER_PKG, "$LSPOSED_MANAGER_PKG.ui.activity.MainActivity"
            )
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { getApplication<Application>().startActivity(i) }
            .onFailure { LogBus.log("[!] 매니저 실행 실패: ${it.message}") }
        kotlinx.coroutines.delay(4500)
        detect()
    }

    /** Re-check su after the user toggled ROOT in the manager. */
    fun recheckGrant() = viewModelScope.launch(Dispatchers.IO) {
        LogBus.log("[*] 루트 권한 재확인")
        suGranted = verifier.suGranted()
        if (suGranted) {
            markStep(1, StepState.DONE, "su 허용됨")
            LogBus.log("[+] 루트 권한 허용됨")
        } else {
            LogBus.log("[!] 아직 미허용 — 매니저에서 ROOT 스위치를 켜세요")
        }
        detect()
    }

    fun softRestart() = viewModelScope.launch(Dispatchers.IO) {
        LogBus.log("[*] 소프트 재시작 요청")
        Notifier.cancelSoftRestart(getApplication())
        _stage.value = RootStage.S6_READY_SOFT_RESTART
        markStep(6, StepState.ACTIVE, "재시작 전달 중")
        val ok = PostRoot.softReboot(reporter, canceller)
        markStep(6, if (ok) StepState.DONE else StepState.FAILED, if (ok) "재시작 전달됨" else "실패")
        LogBus.log(if (ok) "[+] soft-restart 전달됨" else "[!] soft-restart 실패")
    }

    fun oneClick() = viewModelScope.launch(Dispatchers.IO) {
        if (_running.value) return@launch
        cancelled = false
        _steps.value = pendingSteps()
        _running.value = true
        try {
            runFlow()
        } catch (t: Throwable) {
            LogBus.log("[!] 흐름 오류: ${t.message}")
        } finally {
            _running.value = false
            if (!cancelled) detect()
        }
    }

    private fun markStep(index: Int, state: StepState, detail: String? = null) {
        val cur = _steps.value.toMutableList()
        if (index !in cur.indices) return
        cur[index] = cur[index].copy(state = state, detail = detail ?: cur[index].detail)
        _steps.value = cur
    }

    private suspend fun runFlow() {
        val opts = _options.value
        val app = getApplication<Application>()

        // 0 — kernel exploit.
        if (!verifier.rootPresent()) {
            _stage.value = RootStage.S1_ROOTING
            markStep(0, StepState.ACTIVE, "익스플로잇 실행")
            LogBus.log("[*] 커널 익스플로잇 실행 (DirtyFrag)")
            val rc = RootExploit.run(app, reporter)
            if (rc != 0) {
                markStep(0, StepState.FAILED, "실패 rc=$rc")
                LogBus.log("[!] 익스플로잇 실패 rc=$rc")
                _stage.value = RootStage.S0_UNROOTED
                return
            }
        } else {
            LogBus.log("[*] 임시 루트 이미 활성 (/dev/df)")
        }
        markStep(0, StepState.DONE, "임시 루트 활성")
        LogBus.log("[+] 임시 루트 획득")

        // 1 — clean su grant.
        _stage.value = RootStage.S3_AWAITING_ROOT_GRANT
        markStep(1, StepState.ACTIVE, "매니저에서 ROOT 허용 대기")
        if (!grantNotified) { grantNotified = true; Notifier.grantRequest(app) }
        if (!PostRoot.ensureSu(app, KERNELSU_PKG, reporter, canceller)) {
            markStep(1, StepState.ACTIVE, "권한 미허용 — 매니저에서 ROOT 스위치 ON")
            LogBus.log("[!] 루트 권한 미허용 — 매니저에서 이 앱의 ROOT를 켜세요")
            return
        }
        markStep(1, StepState.DONE, "su 허용됨")

        // 2 — detect recommended modules (no install from app).
        _stage.value = RootStage.S4_INSTALLING
        markStep(2, StepState.ACTIVE, "권장 모듈 감지 중")
        LogBus.log("[*] 권장 모듈 감지 중")
        val mods = verifier.moduleStatuses()
        _modules.value = mods
        mods.forEach { LogBus.log("[${if (it.state == ModuleState.INSTALLED) "+" else "!"}] ${it.kind.label}: ${it.detail}") }
        val missing = mods.filter { it.state != ModuleState.INSTALLED }
        if (missing.isNotEmpty()) {
            val names = missing.joinToString(", ") { it.kind.label }
            // Root is already acquired at this point. Do NOT fail the whole flow:
            // a first-time user cannot install modules until after root, so we pause
            // here with guidance and let them resume by pressing ROOT again.
            markStep(2, StepState.ACTIVE, "설치 필요: $names")
            LogBus.log("[+] 루트 획득 완료 — 이제 모듈을 설치하면 됩니다.")
            if (missing.any { it.kind == ModuleKind.MANAGER }) {
                LogBus.log("[!] KernelSU 매니저 앱이 필요합니다. 먼저 설치하세요:")
                LogBus.log("    https://github.com/tiann/KernelSU/releases")
            } else {
                LogBus.log("[!] 설치 필요: $names")
                LogBus.log("[*] KernelSU 매니저 > 모듈 > 설치 에서 Zygisk-Next / LSPosed(zip)를 설치하세요.")
                PostRoot.launchManager(app, KERNELSU_PKG, reporter)
            }
            Notifier.moduleInstallRequired(app, names)
            _stage.value = RootStage.S2_ROOTED_MODULES_MISSING
            return
        }
        markStep(2, StepState.DONE, "3개 모두 감지")

        // 3 — stage KernelSU daemon.
        markStep(3, StepState.ACTIVE, "post-fs-data / services")
        LogBus.log("[*] KernelSU 데몬 스테이지 (post-fs-data / services)")
        PostRoot.stageModules(reporter, false, canceller)
        if (cancelled) return

        // 4..6 — verify each item live.
        _stage.value = RootStage.S5_VERIFYING
        LogBus.log("[*] 검증")
        val rootH = verifier.root()
        val permH = verifier.selinux()
        markStep(3, StepState.DONE, permH.detail)
        val zygH = verifier.zygisk()
        markStep(4, if (zygH.ok) StepState.DONE else StepState.FAILED, zygH.detail)
        val lspH = verifier.lsposed()
        markStep(5, if (lspH.ok) StepState.DONE else StepState.FAILED, lspH.detail)
        val hookH = verifier.hook()
        val h = listOf(rootH, permH, zygH, lspH, hookH)
        _health.value = h
        h.forEach { LogBus.log("[${if (it.ok) "+" else "!"}] ${it.health.label}: ${it.detail}") }
        val coreOk = h.filter { it.health != Health.HOOK && it.health != Health.PERMISSIVE }.all { it.ok }

        // 7 — soft restart to activate modules.
        if (opts.autoSoftRestart) {
            _stage.value = RootStage.S6_READY_SOFT_RESTART
            markStep(6, StepState.ACTIVE, "자동 소프트 재시작")
            LogBus.log("[*] 자동 소프트 재시작 실행 (ksud soft-reboot)")
            val ok = PostRoot.softReboot(reporter, canceller)
            if (cancelled) return
            markStep(6, if (ok) StepState.DONE else StepState.FAILED, if (ok) "재시작 전달됨" else "실패")
            LogBus.log(if (ok) "[+] soft-restart 전달됨" else "[!] soft-restart 실패")
        } else {
            LogBus.log("[*] 자동 소프트 재시작 꺼짐")
            markStep(6, if (coreOk) StepState.DONE else StepState.PENDING, if (coreOk) "재시작 불필요" else "수동 재시작 필요")
            _stage.value = if (coreOk) RootStage.S7_ACTIVE else RootStage.S6_READY_SOFT_RESTART
        }
    }
}
