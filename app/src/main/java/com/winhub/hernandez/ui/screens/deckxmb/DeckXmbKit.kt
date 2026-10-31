package com.winhub.hernandez.ui.screens.deckxmb

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Decoded drawables kept for the process: the XMB home draws the store logos (large PNGs) on every visit, and decoding them
// with painterResource did that on the main thread each time Home opened.
private val xmbLogoCache = ConcurrentHashMap<Int, ImageBitmap>()

/** The longest side (px) a logo is decoded at: plenty for a card, well under the 960 px files. */
private const val XMB_LOGO_EDGE = 512

private fun decodeXmbLogo(resources: Resources, res: Int, maxEdge: Int): ImageBitmap? = xmbLogoCache[res] ?: run {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(resources, res, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
    BitmapFactory.decodeResource(resources, res, BitmapFactory.Options().apply { inSampleSize = sample })
        ?.asImageBitmap()?.also { xmbLogoCache[res] = it }
}

/**
 * [res] decoded off the main thread at no more than [maxEdge] px on its long side. Null until it is ready, and ready at
 * once on every later visit.
 */
@Composable
internal fun rememberXmbLogo(@DrawableRes res: Int, maxEdge: Int = XMB_LOGO_EDGE): ImageBitmap? {
    val resources = LocalContext.current.resources
    return produceState<ImageBitmap?>(xmbLogoCache[res], res) {
        if (value == null) value = withContext(Dispatchers.IO) { decodeXmbLogo(resources, res, maxEdge) }
    }.value
}

/**
 * Sizes as a share of the screen, kept within limits, so the XMB-home layouts keep their proportions from a small phone
 * to a tablet without text or touch targets ending up too small or too large.
 */
@Immutable
internal class ScreenScale(val widthDp: Float, val heightDp: Float) {
    /** [fraction] of the screen width, between [min] and [max]. */
    fun w(fraction: Float, min: Dp = 0.dp, max: Dp = Dp.Infinity): Dp = (widthDp * fraction).dp.coerceIn(min, max)

    /** [fraction] of the screen height, between [min] and [max]. */
    fun h(fraction: Float, min: Dp = 0.dp, max: Dp = Dp.Infinity): Dp = (heightDp * fraction).dp.coerceIn(min, max)

    /** The screen's short side as a factor of a 412 dp phone, held between 0.85 and 1.6: text and icons follow it. */
    val factor: Float = (min(widthDp, heightDp) / 412f).coerceIn(0.85f, 1.6f)

    /** [base] dp as drawn on a 412 dp phone, scaled to this screen. */
    fun dp(base: Float): Dp = (base * factor).dp

    /** [base] sp as drawn on a 412 dp phone, scaled to this screen. */
    fun sp(base: Float): TextUnit = (base * factor).sp
}

@Composable
internal fun rememberScreenScale(): ScreenScale {
    val cfg = LocalConfiguration.current
    return remember(cfg.screenWidthDp, cfg.screenHeightDp) { ScreenScale(cfg.screenWidthDp.toFloat(), cfg.screenHeightDp.toFloat()) }
}

