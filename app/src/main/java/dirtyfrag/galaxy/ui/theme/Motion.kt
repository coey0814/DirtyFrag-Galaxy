package dirtyfrag.galaxy.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/** Specter motion tokens (matches TODO_DIRTYFRAG_APP.md spec). */
val SpecterEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val SpecterEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

object Dur {
    const val FAST = 160
    const val BASE = 220
    const val SLOW = 420
    const val ORB = 700
}
