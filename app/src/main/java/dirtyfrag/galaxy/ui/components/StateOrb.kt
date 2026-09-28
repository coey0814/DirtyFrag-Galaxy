package dirtyfrag.galaxy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dirtyfrag.galaxy.core.RootStage
import dirtyfrag.galaxy.ui.theme.Dur
import dirtyfrag.galaxy.ui.theme.SpecterEase
import dirtyfrag.galaxy.ui.theme.stageColor
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Filament(val angle: Float, val len: Float, val width: Float, val alpha: Float, val curve: Float)
private data class Star(val angle: Float, val radius: Float, val size: Float, val phase: Float, val hue: Int)

/** Continuous prism colour: blends between adjacent palette entries (no stepping). */
private fun prismBlend(offset: Int, phase: Float): Color {
    val n = Prism.size
    val f = (((offset + phase * n) % n) + n) % n
    val i = f.toInt()
    return lerp(Prism[i % n], Prism[(i + 1) % n], f - i)
}

private data class Nebula(val angle: Float, val radius: Float, val size: Float, val hue: Int, val alpha: Float)

private val Prism = listOf(
    Color(0xFFFF6BCB), // pink
    Color(0xFFB57BFF), // violet
    Color(0xFF6C8CFF), // indigo
    Color(0xFF22D3EE), // cyan
    Color(0xFF4ADE80), // green
    Color(0xFFFFD93D), // yellow
    Color(0xFFFF9F45)  // orange
)

/**
 * Specter orb v4 — a translucent jellyfish / plasma / galaxy sphere:
 * limb-brightened base, drifting nebula, spiral arm, two tendril layers,
 * a prismatic rosette and a twinkling starfield.
 */
@Composable
fun StateOrb(stage: RootStage, modifier: Modifier = Modifier, size: Dp = 208.dp) {
    val stageBase by animateColorAsState(stageColor(stage), tween(Dur.ORB, easing = SpecterEase), label = "orbColor")
    val coreScale by animateFloatAsState(
        targetValue = when (stage) {
            RootStage.S1_ROOTING -> 1.06f
            RootStage.S7_ACTIVE -> 1.03f
            RootStage.S6_READY_SOFT_RESTART -> 1.03f
            else -> 1f
        },
        animationSpec = tween(Dur.ORB, easing = SpecterEase), label = "coreScale"
    )

    val filaments = remember {
        List(52) { i ->
            val r = Random(i * 7 + 1)
            Filament(i * (360f / 52f), 0.5f + r.nextFloat() * 0.45f, 0.6f + r.nextFloat() * 1.2f,
                0.14f + r.nextFloat() * 0.45f, (r.nextFloat() - 0.5f) * 0.7f)
        }
    }
    val shortTendrils = remember {
        List(34) { i ->
            val r = Random(i * 17 + 9)
            Filament(i * (360f / 34f) + 6f, 0.18f + r.nextFloat() * 0.32f, 0.8f + r.nextFloat() * 1.4f,
                0.3f + r.nextFloat() * 0.5f, (r.nextFloat() - 0.5f) * 1.1f)
        }
    }
    val stars = remember {
        List(96) { i ->
            val r = Random(i * 13 + 5)
            Star(r.nextFloat() * 360f, 0.12f + r.nextFloat() * 0.85f, 0.7f + r.nextFloat() * 2.0f,
                r.nextFloat(), (r.nextFloat() * Prism.size).toInt())
        }
    }
    val nebulae = remember {
        List(8) { i ->
            val r = Random(i * 29 + 3)
            Nebula(r.nextFloat() * 360f, 0.15f + r.nextFloat() * 0.5f, 0.3f + r.nextFloat() * 0.42f,
                (r.nextFloat() * Prism.size).toInt(), 0.26f + r.nextFloat() * 0.30f)
        }
    }
    val spiral = remember {
        List(56) { i ->
            val r = Random(i * 31 + 11)
            Star(0f, 0f, 0.8f + r.nextFloat() * 1.6f, r.nextFloat(), (r.nextFloat() * Prism.size).toInt())
        }
    }

    val t = rememberInfiniteTransition(label = "orb")
    val float by t.animateFloat(-4f, 4f, infiniteRepeatable(tween(3400, easing = SpecterEase), RepeatMode.Reverse), label = "float")
    val breath by t.animateFloat(0.97f, 1.04f, infiniteRepeatable(tween(1800, easing = SpecterEase), RepeatMode.Reverse), label = "breath")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(26000, easing = LinearEasing)), label = "spin")
    val spin2 by t.animateFloat(360f, 0f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "spin2")
    val shimmer by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(2400, easing = SpecterEase), RepeatMode.Reverse), label = "shimmer")
    val prismShift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "prism")
    val pulse by t.animateFloat(0.9f, 1.18f, infiniteRepeatable(tween(950, easing = SpecterEase), RepeatMode.Reverse), label = "pulse")
    val converge by t.animateFloat(1f, 0.72f, infiniteRepeatable(tween(1900, easing = SpecterEase), RepeatMode.Reverse), label = "converge")
    val rainbow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "rainbow")

    // Fully activated: slowly cycle the base colour through the rainbow (빨주노초파남보).
    // Every other effect is unchanged — only `base` (and everything derived from it) shifts.
    val base = if (stage == RootStage.S7_ACTIVE) {
        Color.hsv(((rainbow % 1f) + 1f) % 1f * 360f, 0.60f, 1f)
    } else {
        stageBase
    }

    val intensity = when (stage) {
        RootStage.S0_UNROOTED -> 0.55f
        RootStage.S7_ACTIVE -> 1f
        RootStage.S6_READY_SOFT_RESTART -> 0.9f
        else -> 0.8f
    }
    val busy = stage.isBusy

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val min = this.size.minDimension
            val cx = center.x
            val cy = center.y + float
            val R = min * 0.33f * coreScale * (if (stage == RootStage.S1_ROOTING) pulse else 1f)
            val c = Offset(cx, cy)

            val light = lerp(base, Color.White, 0.5f)
            val rim = lerp(base, Color.White, 0.88f)
            val dark = lerp(base, Color.Black, 0.34f)

            // Outer halo
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(base.copy(alpha = 0.30f * intensity), Color.Transparent), c, R * 1.95f
                ),
                radius = R * 1.95f, center = c
            )

            // Sphere base (deep translucent core + limb brightening)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to lerp(light, Color.White, 0.15f),
                        0.30f to lerp(base, Color.Black, 0.30f),
                        0.72f to dark,
                        0.93f to base,
                        1f to rim
                    ),
                    center = Offset(cx - R * 0.15f, cy - R * 0.18f), radius = R * 1.35f
                ),
                radius = R, center = c
            )

            val sphere = Path().apply { addOval(Rect(cx - R, cy - R, cx + R, cy + R)) }
            clipPath(sphere) {
                drawNebulae(nebulae, c, R, spin2, prismShift, intensity)
                drawSpiral(spiral, c, R, spin * 1.4f, shimmer)
                drawTendrils(filaments, c, R, spin, shimmer, base, intensity)
                drawShortTendrils(shortTendrils, c, R, spin, shimmer, prismShift)
                drawStars(stars, c, R, spin * 0.4f, shimmer)
                drawRosette(c, R, spin, rim, prismShift, intensity)
            }

            // Limb light
            drawCircle(rim.copy(alpha = 0.30f * intensity), radius = R, center = c, style = Stroke(R * 0.07f))
            drawCircle(rim.copy(alpha = 0.92f), radius = R, center = c, style = Stroke(2.2f))

            if (stage == RootStage.S7_ACTIVE) {
                drawCircle(base.copy(alpha = 0.14f), radius = R * (1.5f + (breath - 1f) * 3f), center = c, style = Stroke(3f))
            }

            if (busy) {
                val cr = R * 1.75f * (if (stage == RootStage.S4_INSTALLING) converge else 1f)
                drawCircle(base.copy(alpha = 0.5f), radius = cr, center = c, style = Stroke(1.6f))
                val dots = 24
                for (i in 0 until dots) {
                    val a = Math.toRadians((spin * 1.5 + i * (360.0 / dots)).toDouble())
                    drawCircle(Prism[i % Prism.size].copy(alpha = 0.9f), radius = 2.2f,
                        center = Offset(cx + (cr * cos(a)).toFloat(), cy + (cr * sin(a)).toFloat()))
                }
            }
        }
    }
}

private fun DrawScope.drawNebulae(
    nebulae: List<Nebula>, c: Offset, R: Float, spin2: Float, prismShift: Float, intensity: Float
) {
    nebulae.forEachIndexed { i, n ->
        val a = Math.toRadians((n.angle + spin2).toDouble())
        val rr = R * n.radius
        val p = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
        val hue = prismBlend(n.hue, prismShift)
        val rad = R * n.size
        drawCircle(
            brush = Brush.radialGradient(
                listOf(hue.copy(alpha = n.alpha * intensity), Color.Transparent), p, rad
            ),
            radius = rad, center = p, blendMode = BlendMode.Screen
        )
    }
}

private fun DrawScope.drawSpiral(stars: List<Star>, c: Offset, R: Float, spin: Float, shimmer: Float) {
    stars.forEachIndexed { i, s ->
        val frac = i.toFloat() / stars.size
        val a = Math.toRadians((frac * 720f + spin).toDouble())
        val rr = R * (0.06f + frac * 0.9f)
        val p = Offset(c.x + (rr * cos(a)).toFloat(), c.y + (rr * sin(a)).toFloat())
        val tw = 0.3f + 0.7f * ((shimmer + s.phase) % 1f)
        drawCircle(Prism[s.hue].copy(alpha = (0.55f * (1f - frac) + 0.12f) * tw), radius = s.size, center = p, blendMode = BlendMode.Plus)
    }
}

private fun DrawScope.drawTendrils(
    filaments: List<Filament>, c: Offset, R: Float, spin: Float, shimmer: Float, base: Color, intensity: Float
) {
    val col = lerp(base, Color.White, 0.7f)
    filaments.forEach { f ->
        val a = Math.toRadians((f.angle + spin).toDouble())
        val len = R * f.len
        val end = Offset(c.x + (len * cos(a)).toFloat(), c.y + (len * sin(a)).toFloat())
        val ma = a + f.curve
        val ctrl = Offset(c.x + (len * 0.5f * cos(ma)).toFloat(), c.y + (len * 0.5f * sin(ma)).toFloat())
        val path = Path().apply { moveTo(c.x, c.y); quadraticBezierTo(ctrl.x, ctrl.y, end.x, end.y) }
        drawPath(path, col.copy(alpha = f.alpha * (0.5f + 0.5f * shimmer) * intensity), style = Stroke(f.width, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawShortTendrils(
    filaments: List<Filament>, c: Offset, R: Float, spin: Float, shimmer: Float, prismShift: Float
) {
    filaments.forEachIndexed { i, f ->
        val a = Math.toRadians((f.angle - spin * 0.6).toDouble())
        val len = R * f.len
        val end = Offset(c.x + (len * cos(a)).toFloat(), c.y + (len * sin(a)).toFloat())
        val ma = a + f.curve
        val ctrl = Offset(c.x + (len * 0.5f * cos(ma)).toFloat(), c.y + (len * 0.5f * sin(ma)).toFloat())
        val hue = prismBlend(i, prismShift)
        val path = Path().apply { moveTo(c.x, c.y); quadraticBezierTo(ctrl.x, ctrl.y, end.x, end.y) }
        drawPath(path, hue.copy(alpha = f.alpha * (0.5f + 0.5f * shimmer)), style = Stroke(f.width, cap = StrokeCap.Round), blendMode = BlendMode.Plus)
    }
}

private fun DrawScope.drawStars(stars: List<Star>, c: Offset, R: Float, spin: Float, shimmer: Float) {
    stars.forEach { s ->
        val a = Math.toRadians((s.angle + spin).toDouble())
        val p = Offset(c.x + (R * s.radius * cos(a)).toFloat(), c.y + (R * s.radius * sin(a)).toFloat())
        val tw = 0.25f + 0.75f * ((shimmer + s.phase) % 1f)
        val col = if (s.hue % 3 == 0) Color.White else Prism[s.hue]
        drawCircle(col.copy(alpha = 0.9f * tw), radius = s.size, center = p, blendMode = BlendMode.Plus)
    }
}

private fun DrawScope.drawRosette(c: Offset, R: Float, spin: Float, rim: Color, prismShift: Float, intensity: Float) {
    val petals = 10
    for (i in 0 until petals) {
        val start = i * (360f / petals) + spin * 0.6f
        val hue = prismBlend(i, prismShift)
        drawArc(
            color = hue.copy(alpha = 0.55f * intensity), startAngle = start, sweepAngle = 30f, useCenter = false,
            topLeft = Offset(c.x - R * 0.27f, c.y - R * 0.27f), size = Size(R * 0.54f, R * 0.54f),
            style = Stroke(width = 2.4f, cap = StrokeCap.Round)
        )
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f * intensity), Color.Transparent), c, R * 0.22f),
        radius = R * 0.22f, center = c
    )
    drawCircle(rim.copy(alpha = 0.5f * intensity), radius = R * 0.14f, center = c, style = Stroke(1.5f))
}
