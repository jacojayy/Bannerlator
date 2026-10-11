package com.winlator.star.store

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * DroidDeck's motion vocabulary, reused so the storefront animates exactly like the launcher:
 * springy scale/lift/elevate on focus, staggered rises on enter. Specs collapse to [snap] when the
 * system animator duration scale is 0 (the user turned animations off), matching the launcher.
 */
internal object DeckMotion {
    /** Live system animator-duration scale; 0 means animations are off. */
    var scale: Float = 1f

    val Ease = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

    fun <T> tw(base: Int, delay: Int = 0, easing: Easing = Ease): FiniteAnimationSpec<T> =
        if (scale == 0f) snap() else tween(base, delay, easing)

    fun <T> sp(
        damping: Float = 0.7f,
        stiffness: Float = Spring.StiffnessMediumLow,
    ): FiniteAnimationSpec<T> = if (scale == 0f) snap() else spring(damping, stiffness)
}

/** Reads the system animator scale once per store screen so [DeckMotion] honours "animations off". */
@Composable
internal fun rememberDeckMotion() {
    val context = LocalContext.current
    DeckMotion.scale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)
}

/**
 * DroidDeck's tile focus motion: the tile scales up, lifts and casts a taller shadow when focused
 * or pressed. The transform origin sits at the bottom-centre so it rises off the surface instead of
 * floating — the same feel as the launcher's game tiles.
 */
@Composable
internal fun Modifier.deckTileFocus(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hot = pressed || focused
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else if (hot) 1.04f else 1f,
        animationSpec = DeckMotion.sp(0.55f, Spring.StiffnessMedium),
        label = "deckTileScale",
    )
    val lift by animateFloatAsState(
        targetValue = if (hot) -5f else 0f,
        animationSpec = DeckMotion.sp(0.6f),
        label = "deckTileLift",
    )
    val elev by animateFloatAsState(
        targetValue = if (hot) 18f else 2f,
        animationSpec = DeckMotion.tw(300),
        label = "deckTileElev",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = lift.dp.toPx()
        shadowElevation = elev.dp.toPx()
        transformOrigin = TransformOrigin(0.5f, 0.9f)
    }
}

/** DroidDeck's staggered list entrance: fade + rise, delayed by [index]. */
@Composable
internal fun DeckRise(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(DeckMotion.tw(450, index * 60)) +
            slideInVertically(DeckMotion.tw(450, index * 60)) { it / 3 },
        exit = fadeOut(DeckMotion.tw(120)),
        modifier = modifier,
        label = "deckRise",
    ) { content() }
}
