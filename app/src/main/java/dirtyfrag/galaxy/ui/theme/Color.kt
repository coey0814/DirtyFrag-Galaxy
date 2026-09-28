package dirtyfrag.galaxy.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dirtyfrag.galaxy.core.RootStage

// ---- Aurora palette (Concept 1) ----
val AuroraBg = Color(0xFF0B0F1E)
val AuroraBg2 = Color(0xFF0E1430)

val Surface1 = Color(0xFF13182A)
val Surface2 = Color(0xFF1A2138)
val Surface3 = Color(0xFF232C4A)

val Purple = Color(0xFF7C5CFF)
val Cyan = Color(0xFF00C2FF)
val Teal = Color(0xFF01E6D0)
val Success = Color(0xFF2EE6A0)
val Warning = Color(0xFFFFB020)
val ErrorRed = Color(0xFFFF6B6B)

val TextPrimary = Color(0xFFEDEAF6)
val TextSecondary = Color(0xFF9AA0B8)
val Outline = Color(0xFF2A3352)

// Back-compat aliases used across the UI.
val SpecterBg = AuroraBg
val Violet = Purple
val Blue = Cyan
val Emerald = Success
val Amber = Warning
val Rose = ErrorRed

// ---- Gradients ----
val AuroraButton = Brush.horizontalGradient(listOf(Purple, Cyan))
val AuroraScreen = Brush.verticalGradient(listOf(AuroraBg, AuroraBg2))
fun orbGradient(base: Color = Purple): Brush = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.9f), base, Cyan)
)

fun stageColor(stage: RootStage): Color = when (stage) {
    RootStage.S0_UNROOTED -> Cyan
    RootStage.S1_ROOTING -> Cyan
    RootStage.S2_ROOTED_MODULES_MISSING -> Success
    RootStage.S3_AWAITING_ROOT_GRANT -> Purple
    RootStage.S4_INSTALLING -> Purple
    RootStage.S5_VERIFYING -> Cyan
    RootStage.S6_READY_SOFT_RESTART -> Teal
    RootStage.S7_ACTIVE -> Success
}

// Rainbow per-step palette (빨주노초파남보).
val StepRed = Color(0xFFFF4D6D)
val StepOrange = Color(0xFFFF9F45)
val StepYellow = Color(0xFFFFD93D)
val StepGreen = Color(0xFF4ADE80)
val StepCyan = Color(0xFF22D3EE)
val StepBlue = Color(0xFF6C8CFF)
val StepViolet = Color(0xFFB57BFF)

fun stepColor(step: dirtyfrag.galaxy.core.RootStep): Color = when (step) {
    dirtyfrag.galaxy.core.RootStep.EXPLOIT -> StepRed
    dirtyfrag.galaxy.core.RootStep.GRANT -> StepOrange
    dirtyfrag.galaxy.core.RootStep.MODULES -> StepYellow
    dirtyfrag.galaxy.core.RootStep.PERMISSIVE -> StepGreen
    dirtyfrag.galaxy.core.RootStep.ZYGISK -> StepCyan
    dirtyfrag.galaxy.core.RootStep.LSPOSED -> StepBlue
    dirtyfrag.galaxy.core.RootStep.RESTART -> StepViolet
}
