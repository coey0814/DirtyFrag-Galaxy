package dirtyfrag.galaxy.core

/** Recommended stack — installed/vouched for by the user, this app only detects them. */
const val KERNELSU_PKG = "me.weishu.kernelsu"
const val LSPOSED_MANAGER_PKG = "org.lsposed.manager"

/** App state machine. */
enum class RootStage {
    S0_UNROOTED,
    S1_ROOTING,
    S2_ROOTED_MODULES_MISSING,
    S3_AWAITING_ROOT_GRANT,
    S4_INSTALLING,
    S5_VERIFYING,
    S6_READY_SOFT_RESTART,
    S7_ACTIVE;

    val isBusy get() = this == S1_ROOTING || this == S4_INSTALLING || this == S5_VERIFYING
}

/** Ordered root flow, surfaced as a checklist with per-step check marks. */
enum class RootStep(val label: String) {
    EXPLOIT("커널 익스플로잇"),
    GRANT("루트 권한 부여"),
    MODULES("모듈 감지"),
    PERMISSIVE("SELinux"),
    ZYGISK("Zygisk 주입"),
    LSPOSED("LSPosed 데몬"),
    RESTART("소프트 재시작")
}

enum class StepState { PENDING, ACTIVE, DONE, FAILED }

data class StepStatus(val step: RootStep, val state: StepState, val detail: String = "")

enum class ModuleKind(val label: String) { MANAGER("KernelSU"), LSPOSED("LSPosed"), ZYGISK("Zygisk-Next") }

enum class ModuleState { NOT_INSTALLED, INSTALLED, FAILED }

data class ModuleStatus(
    val kind: ModuleKind,
    val state: ModuleState = ModuleState.NOT_INSTALLED,
    val detail: String = ""
)

enum class Health(val label: String) { ROOT("Root"), PERMISSIVE("SELinux"), ZYGISK("Zygisk"), LSPOSED("LSPosed"), HOOK("Module hook") }

data class HealthStatus(val health: Health, val ok: Boolean, val detail: String = "")

data class DeviceInfo(
    val model: String = "",
    val firmware: String = "",
    val androidRelease: String = "",
    val kernel: String = ""
)

/** Options chosen by the user. */
data class Options(
    val autoRootOnBoot: Boolean = false,
    val autoSoftRestart: Boolean = false,
    val restoreEnforcing: Boolean = true
)

/** All steps in a fresh, un-started state. */
fun pendingSteps(): List<StepStatus> = RootStep.entries.map { StepStatus(it, StepState.PENDING) }

/** Compute the checklist state for the current stage / detection results. */
fun rootSteps(
    stage: RootStage,
    suGranted: Boolean,
    modulesOk: Boolean,
    rootPresent: Boolean,
    health: List<HealthStatus>
): List<StepStatus> {
    val byHealth = health.associateBy { it.health }
    fun ok(h: Health) = byHealth[h]?.ok == true
    fun healthStep(step: RootStep, h: Health): StepStatus {
        val st = byHealth[h]
        val state = when {
            st?.ok == true -> StepState.DONE
            stage == RootStage.S5_VERIFYING -> StepState.ACTIVE
            stage == RootStage.S6_READY_SOFT_RESTART -> StepState.FAILED
            else -> StepState.PENDING
        }
        val detail = if (state == StepState.DONE || state == StepState.FAILED) (st?.detail ?: "") else ""
        return StepStatus(step, state, detail)
    }

    return listOf(
        StepStatus(
            RootStep.EXPLOIT,
            when {
                rootPresent -> StepState.DONE
                stage == RootStage.S1_ROOTING -> StepState.ACTIVE
                else -> StepState.PENDING
            },
            if (rootPresent) "임시 루트 활성" else ""
        ),
        StepStatus(
            RootStep.GRANT,
            when {
                suGranted -> StepState.DONE
                stage == RootStage.S3_AWAITING_ROOT_GRANT -> StepState.ACTIVE
                else -> StepState.PENDING
            },
            if (suGranted) "su 허용됨" else if (stage == RootStage.S3_AWAITING_ROOT_GRANT) "매니저에서 허용 대기" else ""
        ),
        StepStatus(
            RootStep.MODULES,
            when {
                modulesOk -> StepState.DONE
                stage == RootStage.S4_INSTALLING -> StepState.ACTIVE
                // Missing modules is NOT a failure: on a first run the user must
                // install them (needs root, which is already acquired) and resume.
                stage == RootStage.S2_ROOTED_MODULES_MISSING -> StepState.ACTIVE
                else -> StepState.PENDING
            },
            if (modulesOk) "3개 모두 감지" else if (stage == RootStage.S2_ROOTED_MODULES_MISSING) "KernelSU 매니저에서 설치" else ""
        ),
        // SELinux is informational: some devices (e.g. SM-S931N / 6.6.98) complete
        // root + Zygisk + LSPosed while fully Enforcing, so it must never fail S7.
        StepStatus(
            RootStep.PERMISSIVE,
            when {
                byHealth[Health.PERMISSIVE] != null -> StepState.DONE
                stage == RootStage.S5_VERIFYING -> StepState.ACTIVE
                else -> StepState.PENDING
            },
            byHealth[Health.PERMISSIVE]?.detail ?: ""
        ),
        healthStep(RootStep.ZYGISK, Health.ZYGISK),
        healthStep(RootStep.LSPOSED, Health.LSPOSED),
        StepStatus(
            RootStep.RESTART,
            when {
                stage == RootStage.S7_ACTIVE -> StepState.DONE
                stage == RootStage.S6_READY_SOFT_RESTART -> StepState.ACTIVE
                else -> StepState.PENDING
            },
            if (stage == RootStage.S7_ACTIVE) "활성화 완료" else ""
        )
    )
}
