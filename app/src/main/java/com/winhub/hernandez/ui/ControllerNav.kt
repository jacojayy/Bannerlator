package com.winhub.hernandez.ui

import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationInstance
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.winhub.hernandez.ui.theme.AppThemeState

/** Live theme accent, used as the default focus-outline color. */
private val FocusOutlineColor get() = Color(AppThemeState.getCurrentAccentArgb())

/** Root node that owns fallback D-pad focus for the window; screens re-seed it on navigation. */
val LocalControllerRootFocus = staticCompositionLocalOf { FocusRequester() }

/**
 * Global focus indication: a purple rounded outline around whatever Compose node currently holds
 * focus, plus a light wash while it is pressed. Provided via `LocalIndication`, so it applies to
 * every `clickable`/`focusable` in the app — and to dialogs, which inherit composition locals.
 * This replaces the ripple.
 */
class ControllerFocusIndication(private val outline: Color = FocusOutlineColor) : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val focused = interactionSource.collectIsFocusedAsState()
        val pressed = interactionSource.collectIsPressedAsState()
        // Keyed on the colour too: a theme change swaps the shell's indication, but the
        // interaction source is unchanged, so keying only on it kept the old ring colour until
        // the control left composition (the "theme only applies after a tab switch" bug).
        val instance = remember(interactionSource, outline) { ControllerFocusInstance(outline) }
        instance.focused = focused
        instance.pressed = pressed
        return instance
    }
}

private class ControllerFocusInstance(private val color: Color) : IndicationInstance {
    var focused: State<Boolean> = mutableStateOf(false)
    var pressed: State<Boolean> = mutableStateOf(false)

    override fun ContentDrawScope.drawIndication() {
        drawContent()
        if (pressed.value) {
            drawRect(color = color.copy(alpha = 0.16f))
        }
        if (focused.value) {
            val stroke = 3.dp.toPx()
            val half = stroke / 2f
            val radius = 10.dp.toPx()
            drawRoundRect(
                color = color,
                topLeft = Offset(half, half),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = stroke),
            )
        }
    }
}

/**
 * Move focus without letting Compose 1.6.2's empty-focus crash escape. For a frame after a screen
 * transition swaps the focused node (the + button, a card, the tab host) the owner has no active
 * focus target, and [FocusManager.moveFocus] then throws "FocusRequester is not initialized" from
 * inside the focus owner — an app-killing IllegalStateException on the next joystick nudge.
 * Callers treat a failed move as "nothing to move to".
 */
internal fun FocusManager.moveFocusSafely(direction: FocusDirection): Boolean =
    runCatching { moveFocus(direction) }.getOrDefault(false)

/**
 * Walks the window with the D-pad. Compose never maps direction keys onto focus movement, and a
 * key is only dispatched to modifiers when some node inside the tree holds focus — so this root
 * box holds focus when a screen change drops it, then forwards direction keys to the focus system.
 * Screens with their own D-pad handling claim the key first (`onPreviewKeyEvent` runs before this
 * bubble handler), so custom surfaces like XMB keep priority.
 */
@Composable
fun ControllerNavRoot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    val rootFocus = remember { FocusRequester() }
    // True only while this invisible root holds focus (i.e. a screen change just dropped it).
    var rootFocused by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalControllerRootFocus provides rootFocus) {
        Box(
            modifier
                .fillMaxSize()
                .focusRequester(rootFocus)
                .onFocusChanged { rootFocused = it.isFocused }
                .focusable()
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val dir = when (e.key) {
                        Key.DirectionUp -> FocusDirection.Up
                        Key.DirectionDown -> FocusDirection.Down
                        Key.DirectionLeft -> FocusDirection.Left
                        Key.DirectionRight -> FocusDirection.Right
                        else -> null
                    } ?: return@onKeyEvent false
                    // Reseed ONLY when the root itself is focused (screen just changed). If a real
                    // control has focus, a failed direction must do nothing: falling back to `Next`
                    // is what made Left walk DOWN the list when there was no target to the left.
                    val moved = runCatching { focusManager.moveFocus(dir) }.getOrElse {
                        // No active focus target (a screen change is mid-flight). Reseed the root
                        // so the next press has somewhere to start from, and swallow this one.
                        runCatching { rootFocus.requestFocus() }
                        false
                    }
                    if (moved) true
                    else if (rootFocused) focusManager.moveFocusSafely(FocusDirection.Next)
                    else false
                },
        ) { content() }
    }
}
