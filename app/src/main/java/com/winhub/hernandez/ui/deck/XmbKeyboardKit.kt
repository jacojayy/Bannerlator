package com.winhub.hernandez.ui.deck

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.winhub.hernandez.R
import kotlin.math.min

// What the XMB keyboard borrows from the Deck style: its type, density, focus ring and glyph kinds.

/** How much smaller the Deck style draws than the rest of the app. */
internal const val DECK_UI_SCALE = 0.8f

/** The Deck density, set by whoever hosts a Deck-styled window; null = use the raw density scaled by [DECK_UI_SCALE]. */
internal val LocalDeckDensity = staticCompositionLocalOf<Density?> { null }

internal val NunitoSansFamily = FontFamily(
    Font(R.font.nunito_sans_regular, FontWeight.Normal),
    Font(R.font.nunito_sans_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_sans_bold, FontWeight.Bold),
    Font(R.font.nunito_sans_extrabold, FontWeight.ExtraBold),
)

/** Hairline around cards and bars. */
@Composable
internal fun deckLine(): Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)

/** Which face a controller button glyph uses. Face buttons are round, bumpers are keycaps. */
internal enum class GlyphKind { A, B, X, Y, BUMPER, PLAIN }

/**
 * The Deck focus treatment: the focused element scales up and gets an accent outline with a soft
 * accent glow. Put it BEFORE the clickable/focusable modifier so it can observe that node's focus.
 */
@Composable
internal fun Modifier.deckFocusRing(
    shape: Shape,
    scaleTo: Float = 1.05f,
    onFocused: (() -> Unit)? = null,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) scaleTo else 1f, label = "deckFocusScale")
    val accent = MaterialTheme.colorScheme.primary
    return this
        .onFocusChanged {
            val now = it.isFocused
            if (now && !focused) onFocused?.invoke()
            focused = now
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .then(
            if (focused) Modifier
                .shadow(18.dp, shape, ambientColor = accent, spotColor = accent)
                .border(3.dp, accent, shape)
            else Modifier
        )
}

/**
 * Sizes as a share of the screen, kept within limits, so the keyboard keeps its proportions from a small phone
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

/** In a dialog window: keep the status and navigation bars hidden there too (a swipe in from the edge shows them briefly). */
@Composable
internal fun DeckDialogBars() {
    val view = LocalView.current
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    DisposableEffect(dialogWindow, view) {
        if (dialogWindow != null) {
            val controller = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {}
    }
}
