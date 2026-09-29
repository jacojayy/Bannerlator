package com.winlator.star.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

/** Amethyst (ThemePreset "Amethyst" primary) — the purple used for the focus outline. */
private val FocusOutlineColor = Color(0xFFA855F7)

/** Root node that owns fallback D-pad focus for the window; screens re-seed it on navigation. */
val LocalControllerRootFocus = staticCompositionLocalOf { FocusRequester() }

/**
 * Global focus indication: a purple rounded outline around whatever Compose node currently holds
 * focus, plus a light wash while it is pressed. Provided via `LocalIndication`, so it applies to
 * every `clickable`/`focusable` in the app — and to dialogs, which inherit composition locals.
 * This replaces the ripple.
 */
class ControllerFocusIndication : Indication {
    @Composable
    override fun rememberUpdatedInstance(interactionSource: InteractionSource): IndicationInstance {
        val focused = interactionSource.collectIsFocusedAsState()
        val pressed = interactionSource.collectIsPressedAsState()
        val instance = remember(interactionSource) { ControllerFocusInstance() }
        instance.focused = focused
        instance.pressed = pressed
        return instance
    }
}

private class ControllerFocusInstance : IndicationInstance {
    var focused: State<Boolean> = mutableStateOf(false)
    var pressed: State<Boolean> = mutableStateOf(false)

    override fun ContentDrawScope.drawIndication() {
        drawContent()
        if (pressed.value) {
            drawRect(color = FocusOutlineColor.copy(alpha = 0.16f))
        }
        if (focused.value) {
            val stroke = 3.dp.toPx()
            val half = stroke / 2f
            val radius = 10.dp.toPx()
            drawRoundRect(
                color = FocusOutlineColor,
                topLeft = Offset(half, half),
                size = Size(size.width - stroke, size.height - stroke),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = stroke),
            )
        }
    }
}

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
    CompositionLocalProvider(LocalControllerRootFocus provides rootFocus) {
        Box(
            modifier
                .fillMaxSize()
                .focusRequester(rootFocus)
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
                    // `Next` is the reseed: focus was just dropped on this root (screen changed),
                    // so nothing is focused yet and the direction should enter the tree instead.
                    focusManager.moveFocus(dir) || focusManager.moveFocus(FocusDirection.Next)
                },
        ) { content() }
    }
}
