package dirtyfrag.galaxy.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dirtyfrag.galaxy.ui.theme.AuroraBg
import dirtyfrag.galaxy.ui.theme.AuroraBg2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen aurora + starfield background — single file.
 *
 * The aurora is NOT computed per frame. The entire seamless 15 s loop is baked
 * once into a tiny sprite sheet (40 keyframes of 96×144 px, ~2.2 MB total) and
 * playback is just two scaled bitmap blits per frame (current keyframe + next,
 * additively cross-faded). That is why it is smooth:
 *
 *  - per frame: 2 full-screen blits + ~340 pre-baked star sprites + 2 rects;
 *    no shaders, no blur, no per-pixel work, no allocations → ~1–3 ms GPU;
 *  - the sheet is baked on Dispatchers.Default, so even the one-time cost never
 *    touches the main thread (the aurora fades in when ready);
 *  - one `withFrameNanos` loop throttled to [TARGET_FPS], time read only in the
 *    draw phase → no recomposition; own `graphicsLayer` → sibling animations
 *    never re-record the sky;
 *  - every baked sine term uses an integer time multiplier → frame 40 is
 *    pixel-identical to frame 0 → perfectly seamless loop (and 3600 % 15 == 0
 *    keeps the hourly time wrap seamless too).
 */
private const val TARGET_FPS = 30          // 60 also works fine with this design
private const val GLOW_PX = 48
private const val STAR_COUNT = 340

// ---- aurora bake config ----
private const val AURORA_FRAMES = 40       // keyframes in the seamless loop
private const val AURORA_LOOP_S = 15f      // loop length; must divide 3600 evenly
private const val AURORA_W = 96            // baked frame width  (bilinear-stretched to screen)
private const val AURORA_H = 144           // baked frame height
private const val AURORA_SHEET_COLS = 8    // sheet layout: 8 × 5 frames
private const val AURORA_SHEET_ROWS = 5

@Composable
fun AuroraFlow(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val time = rememberStarfieldTime(TARGET_FPS)
    Box(modifier) {
        AuroraSky(time, intensity, Modifier.fillMaxSize())
    }
}

/** Monotonic seconds since first frame, throttled to [fps], wrapped for float precision. */
@Composable
fun rememberStarfieldTime(fps: Int = TARGET_FPS): State<Float> {
    val state = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fps) {
        var start = 0L
        var last = 0L
        val step = 1_000_000_000L / fps.coerceAtLeast(1)
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) { start = now; last = now }
                if (now - last >= step) {
                    last = now
                    state.floatValue = ((now - start) / 1_000_000_000f) % 3600f
                }
            }
        }
    }
    return state
}

// ---------------------------------------------------------------------------
// Aurora — pre-baked keyframe sheet
// ---------------------------------------------------------------------------

private class AuroraSheet(
    val image: ImageBitmap,
    val frameW: Int,
    val frameH: Int,
) {
    val frameSize = IntSize(frameW, frameH)

    fun srcOf(frame: Int): IntOffset {
        val c = frame % AURORA_SHEET_COLS
        val r = frame / AURORA_SHEET_COLS
        return IntOffset(c * frameW, r * frameH)
    }
}

/**
 * Renders the whole aurora loop offline into one bitmap sheet.
 *
 * Model per baked frame (phase p ∈ 0..1):
 *  - a bright green "curtain" whose wavy lower rim sits around 35–70 % of the
 *    screen height, sharp below the rim (with a faint pink fringe), decaying
 *    upward through teal → violet → indigo;
 *  - vertical rays + fine striations from higher-frequency x terms;
 *  - a slower, broader teal veil underneath for depth.
 * All temporal terms are integer multiples of the loop → seamless.
 */
private fun bakeAuroraSheet(): AuroraSheet {
    val w = AURORA_W
    val h = AURORA_H
    val tau = (2.0 * PI).toFloat()

    // exp(-q) LUT for q ∈ [0, 6] — every vertical falloff in the texture.
    val expN = 512
    val expLut = FloatArray(expN) { i -> exp(-(i.toFloat() / (expN - 1)) * 6f) }
    val expK = (expN - 1) / 6f

    // Curtain color by height above the rim: green → teal → violet → indigo.
    val lutN = 65
    val lutR = FloatArray(lutN)
    val lutG = FloatArray(lutN)
    val lutB = FloatArray(lutN)
    run {
        val kT = floatArrayOf(0f, 0.22f, 0.45f, 0.70f, 1f)
        val kR = floatArrayOf(150f, 96f, 70f, 135f, 95f)
        val kG = floatArrayOf(255f, 240f, 195f, 120f, 80f)
        val kB = floatArrayOf(175f, 200f, 215f, 240f, 170f)
        var k = 0
        for (i in 0 until lutN) {
            val q = i / (lutN - 1).toFloat()
            while (k < kT.size - 2 && q > kT[k + 1]) k++
            val f = ((q - kT[k]) / (kT[k + 1] - kT[k])).coerceIn(0f, 1f)
            lutR[i] = kR[k] + (kR[k + 1] - kR[k]) * f
            lutG[i] = kG[k] + (kG[k + 1] - kG[k]) * f
            lutB[i] = kB[k] + (kB[k + 1] - kB[k]) * f
        }
    }

    val sheet = Bitmap.createBitmap(
        w * AURORA_SHEET_COLS, h * AURORA_SHEET_ROWS, Bitmap.Config.ARGB_8888
    )
    val px = IntArray(w * h)

    val edge = FloatArray(w)    // wavy lower rim of the bright curtain
    val ray = FloatArray(w)     // ray / striation intensity per column
    val vEdge = FloatArray(w)   // broad faint veil rim
    val vRay = FloatArray(w)

    for (frame in 0 until AURORA_FRAMES) {
        val p = frame / AURORA_FRAMES.toFloat()

        // ---- per-column terms (sins are per-column, never per-pixel) ----
        var x = 0
        while (x < w) {
            val u = x / w.toFloat()
            val w1 = sin(tau * (u * 3f + p * 2f) + 0.8f)
            val w2 = sin(tau * (u * 5f - p * 3f) + 2.1f)
            val w3 = sin(tau * (u * 8f + p * 5f) + 4.4f)
            edge[x] = 0.52f + 0.09f * w1 + 0.05f * w2 + 0.04f * w3

            var rr = 0.55f +
                0.30f * sin(tau * (u * 7f + p * 3f) + 1.0f) +
                0.22f * sin(tau * (u * 13f - p * 4f) + 3.6f) +
                0.14f * sin(tau * (u * 23f + p * 6f) + 5.2f)
            rr = rr.coerceIn(0f, 1f)
            ray[x] = rr * rr

            vEdge[x] = 0.66f + 0.06f * sin(tau * (u * 2f + p) + 5.1f)
            var vv = 0.45f +
                0.35f * sin(tau * (u * 2f - p) + 2.6f) +
                0.20f * sin(tau * (u * 3f + p * 2f) + 0.7f)
            vv = vv.coerceIn(0f, 1f)
            vRay[x] = vv * vv
            x++
        }

        // ---- per-pixel fill (LUT lookups + multiplies only) ----
        val invH = 1f / (h - 1)
        var y = 0
        while (y < h) {
            val v = y * invH
            var i = y * w
            var x2 = 0
            while (x2 < w) {
                val e = edge[x2]
                var mA: Float
                var rM: Float
                var gM: Float
                var bM: Float
                if (v >= e) {
                    // below the rim: quick fade-out + faint pink fringe
                    val below = v - e
                    mA = expLut[((below * 18f) * expK).toInt().coerceIn(0, expN - 1)]
                    val fr = (1f - below * 40f).coerceIn(0f, 1f)
                    rM = 150f + 105f * fr
                    gM = 255f - 125f * fr
                    bM = 175f - 10f * fr
                } else {
                    // above the rim: slow decay upward, hue shifts with height
                    val d = e - v
                    mA = expLut[((d * 3.2f) * expK).toInt().coerceIn(0, expN - 1)]
                    val hh = (d * 1.818f).coerceAtMost(1f) * (lutN - 1)
                    val ci = hh.toInt().coerceIn(0, lutN - 2)
                    val cf = hh - ci
                    rM = lutR[ci] + (lutR[ci + 1] - lutR[ci]) * cf
                    gM = lutG[ci] + (lutG[ci + 1] - lutG[ci]) * cf
                    bM = lutB[ci] + (lutB[ci + 1] - lutB[ci]) * cf
                }
                mA *= 0.30f + 0.70f * ray[x2]   // rays stay connected, no black gaps
                mA *= 0.92f

                val ev = vEdge[x2]
                val vA = (if (v >= ev) {
                    expLut[(((v - ev) * 18f) * expK).toInt().coerceIn(0, expN - 1)]
                } else {
                    expLut[(((ev - v) * 2.4f) * expK).toInt().coerceIn(0, expN - 1)]
                }) * vRay[x2] * 0.50f

                val aT = mA + vA
                if (aT > 0.004f) {
                    val inv = 1f / aT
                    val r = ((rM * mA + 80f * vA) * inv + 0.5f).toInt().coerceIn(0, 255)
                    val g = ((gM * mA + 220f * vA) * inv + 0.5f).toInt().coerceIn(0, 255)
                    val b = ((bM * mA + 205f * vA) * inv + 0.5f).toInt().coerceIn(0, 255)
                    val a = (aT * 255f + 0.5f).toInt().coerceIn(0, 255)
                    px[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                } else {
                    px[i] = 0
                }
                i++
                x2++
            }
            y++
        }

        sheet.setPixels(
            px, 0, w,
            (frame % AURORA_SHEET_COLS) * w,
            (frame / AURORA_SHEET_COLS) * h,
            w, h
        )
    }
    return AuroraSheet(sheet.asImageBitmap(), w, h)
}

// ---- star model (deterministic, built once) ----

private class SkyStar(
    val x: Float,          // 0..1
    val y: Float,          // 0..1
    val size: Float,       // glow sprite size in dp-ish px multiplier
    val mag: Float,        // 0..1 brightness
    val phase: Float,      // twinkle phase
    val period: Float,     // twinkle period (s)
    val amp: Float,        // twinkle depth 0..1
    val flare: Float,      // rare sparkle period (s)
    val tint: Int,
)

private fun makeStars(count: Int, seed: Int): List<SkyStar> {
    val rnd = Random(seed)
    return List(count) {
        val m = rnd.nextFloat().let { it * it * it }          // bias to dim
        SkyStar(
            x = rnd.nextFloat(),
            y = rnd.nextFloat(),
            size = 1.0f + m * 2.4f,
            mag = m,
            phase = rnd.nextFloat() * 2f * PI.toFloat(),
            period = 1.6f + rnd.nextFloat().let { it * it } * 9.0f,
            amp = 0.10f + rnd.nextFloat() * 0.85f,
            flare = 10f + rnd.nextFloat() * 26f,
            tint = rnd.nextInt(4),
        )
    }
}

@Composable
private fun AuroraSky(time: State<Float>, intensity: Float, modifier: Modifier) {
    val stars = remember { makeStars(STAR_COUNT, seed = 2026) }
    // Tints: white, cool blue, warm, pale cyan — baked once.
    val sprites = remember {
        listOf(
            glowSprite(Color(0xFFFFFFFF)),
            glowSprite(Color(0xFFD6E6FF)),
            glowSprite(Color(0xFFFFEBCF)),
            glowSprite(Color(0xFFD8F3FF)),
        )
    }
    val srcSize = remember { IntSize(GLOW_PX, GLOW_PX) }

    // Bake once, off the main thread. Stars are visible immediately; the aurora
    // fades in smoothly when the sheet is ready.
    val sheetState = remember { mutableStateOf<AuroraSheet?>(null) }
    val revealAt = remember { mutableFloatStateOf(-1f) }
    LaunchedEffect(Unit) {
        val baked = withContext(Dispatchers.Default) { bakeAuroraSheet() }
        revealAt.floatValue = time.value
        sheetState.value = baked
    }

    Canvas(modifier.graphicsLayer()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val t = time.value

        // Deep-navy sky.
        drawRect(Brush.verticalGradient(listOf(AuroraBg, AuroraBg2)))

        // ---- aurora: two additive blits of pre-baked keyframes ----
        val sheet = sheetState.value
        val ra = revealAt.floatValue
        if (sheet != null && ra >= 0f) {
            var rv = ((t - ra) / 0.9f).coerceIn(0f, 1f)
            rv = rv * rv * (3f - 2f * rv)                     // smoothstep fade-in
            val a = rv * intensity
            if (a > 0.001f) {
                val fi = ((t / AURORA_LOOP_S) % 1f) * AURORA_FRAMES
                val i0 = fi.toInt().coerceIn(0, AURORA_FRAMES - 1)
                val f = fi - i0
                val dst = IntSize(w.toInt(), h.toInt())
                drawImage(
                    image = sheet.image,
                    srcOffset = sheet.srcOf(i0), srcSize = sheet.frameSize,
                    dstOffset = IntOffset.Zero, dstSize = dst,
                    alpha = (1f - f) * a,
                    blendMode = BlendMode.Plus,
                    filterQuality = FilterQuality.Low,
                )
                if (f > 0.0001f) {
                    val i1 = if (i0 + 1 == AURORA_FRAMES) 0 else i0 + 1
                    drawImage(
                        image = sheet.image,
                        srcOffset = sheet.srcOf(i1), srcSize = sheet.frameSize,
                        dstOffset = IntOffset.Zero, dstSize = dst,
                        alpha = f * a,
                        blendMode = BlendMode.Plus,
                        filterQuality = FilterQuality.Low,
                    )
                }
            }
        }

        // ---- stars (unchanged) ----
        val unit = h / 900f   // keep star sizes stable across screen sizes

        stars.forEach { s ->
            val w1 = 0.5f + 0.5f * sin(s.phase + (t / s.period) * 2f * PI.toFloat())
            var bright = 1f - s.amp + s.amp * w1

            if (s.mag > 0.55f) {
                val imp = sin(s.phase * 1.7f + (t / s.flare) * 2f * PI.toFloat()).coerceAtLeast(0f)
                bright += imp.pow(24) * 0.7f
            }

            val alpha = (bright * (0.22f + 0.78f * s.mag) * intensity).coerceIn(0f, 1f)
            if (alpha <= 0.012f) return@forEach

            val rPx = s.size * unit * 3f
            val sizePx = (rPx * 2f).toInt().coerceAtLeast(3)
            drawImage(
                image = sprites[s.tint],
                srcOffset = IntOffset.Zero, srcSize = srcSize,
                dstOffset = IntOffset((s.x * w - rPx).toInt(), (s.y * h - rPx).toInt()),
                dstSize = IntSize(sizePx, sizePx),
                alpha = alpha,
                blendMode = BlendMode.Plus
            )
        }

        // Subtle depth vignette so the field does not look flat.
        drawRect(
            Brush.radialGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                center = Offset(w / 2f, h * 0.45f), radius = w * 0.95f
            )
        )
    }
}

/** A soft radial glow baked once as a bitmap and reused for every star. */
private fun glowSprite(color: Color): ImageBitmap {
    val s = GLOW_PX
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val c = color.toArgb()
    paint.shader = RadialGradient(
        s / 2f, s / 2f, s / 2f,
        intArrayOf(
            c,
            color.copy(alpha = 0.42f).toArgb(),
            color.copy(alpha = 0.14f).toArgb(),
            color.copy(alpha = 0.04f).toArgb(),
            color.copy(alpha = 0f).toArgb()
        ),
        floatArrayOf(0f, 0.18f, 0.40f, 0.70f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(s / 2f, s / 2f, s / 2f, paint)
    return bmp.asImageBitmap()
}