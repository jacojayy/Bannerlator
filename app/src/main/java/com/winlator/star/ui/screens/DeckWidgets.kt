package com.winlator.star.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** DroidDeck's Graphite palette, used by the Deck Mode widgets (same hex values as ui/Theme.kt). */
internal object DeckPalette {
    val background = Color(0xFF0A0B0D)
    val surface = Color(0xFF121417)
    val surfaceVariant = Color(0xFF1A1D22)
    val line = Color(0xFF262A31)
    val line2 = Color(0xFF343A43)
    val onBackground = Color(0xFFF2F4F7)
    val onSurfaceVariant = Color(0xFF9AA3AF)
    val primary = Color(0xFF1A9FFF)
    val onPrimary = Color(0xFF03111F)
    val good = Color(0xFF4CD37F)
    val error = Color(0xFFFF8A80)

    /** DroidDeck's GroupShape. */
    val groupShape = RoundedCornerShape(14.dp)

    fun titleCase(value: String): String =
        value.lowercase().split("_").joinToString(" ") { part ->
            part.replaceFirstChar { ch -> ch.uppercaseChar() }
        }
}

/**
 * DroidDeck's settings group — an uppercase eyebrow with a hairline rule, then the rows in a
 * 14dp card. Lifted from DroidDeck ui/SettingsWidgets.kt `SettingsGroup`.
 */
@Composable
internal fun DeckGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 6.dp),
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            color = DeckPalette.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(DeckPalette.line),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeckPalette.groupShape)
            .background(DeckPalette.surface)
            .border(1.dp, DeckPalette.line, DeckPalette.groupShape),
        content = content,
    )
}

/**
 * One setting: label and optional hint on the left, [control] on the right, hairline under it.
 * DroidDeck's `SettingsRow`.
 */
@Composable
internal fun DeckRow(label: String, hint: String? = null, control: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeckPalette.onBackground,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    fontSize = 12.5.sp,
                    color = DeckPalette.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        control()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DeckPalette.line),
    )
}

/** DroidDeck's on/off switch: a 52×30 pill track with a 22dp knob. */
@Composable
internal fun DeckToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val knob by animateFloatAsState(if (checked) 1f else 0f, label = "deckToggle")
    val track = if (checked) DeckPalette.primary else DeckPalette.surfaceVariant
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 64.dp)
            .clickable { onChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 30.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(track)
                .border(
                    width = 1.dp,
                    color = if (checked) DeckPalette.primary else DeckPalette.line2,
                    shape = RoundedCornerShape(99.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(22.dp)
                    .graphicsLayer { translationX = knob * 22.dp.toPx() }
                    .clip(CircleShape)
                    .background(if (checked) DeckPalette.onPrimary else DeckPalette.onSurfaceVariant),
            )
        }
    }
}

/** DroidDeck's value chip: the current value in a pill with a caret that flips when open. */
@Composable
internal fun DeckValueChip(text: String, open: Boolean, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "deckCaret")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .widthIn(min = 120.dp)
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DeckPalette.surfaceVariant)
            .border(1.dp, DeckPalette.line2, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeckPalette.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        Text(
            text = "▾",
            fontSize = 10.sp,
            color = DeckPalette.onSurfaceVariant,
            modifier = Modifier.rotate(rot),
        )
    }
}

/** DroidDeck's choice row: label + hint with a value chip that opens a list of options. */
@Composable
internal fun DeckChoiceRow(
    label: String,
    hint: String? = null,
    options: List<Pair<String, String>>,
    selected: String,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    DeckRow(label, hint) {
        Box {
            DeckValueChip(
                text = options.firstOrNull { it.first == selected }?.second ?: selected,
                open = open,
                onClick = { open = !open },
            )
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier.background(DeckPalette.surfaceVariant),
            ) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = text,
                                color = if (value == selected) DeckPalette.primary else DeckPalette.onBackground,
                                fontSize = 14.sp,
                                fontWeight = if (value == selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        onClick = {
                            onPick(value)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

/** A value as a track: DroidDeck's SliderRow shape with a Material slider inside. */
@Composable
internal fun DeckSliderRow(
    label: String,
    hint: String? = null,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeckPalette.onBackground,
                )
                if (hint != null) {
                    Text(
                        text = hint,
                        fontSize = 12.5.sp,
                        color = DeckPalette.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Text(
                text = format(value),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeckPalette.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DeckPalette.line),
        )
    }
}

/** A label + editable text field in the Deck style (same TextField colors used across Deck). */
@Composable
internal fun DeckTextRow(
    label: String,
    hint: String? = null,
    value: String,
    onValueChange: (String) -> Unit,
    width: Dp = 300.dp,
    mask: Boolean = false,
) {
    DeckRow(label, hint) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (mask && value.isNotEmpty()) PasswordVisualTransformation() else VisualTransformation.None,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DeckPalette.surfaceVariant,
                unfocusedContainerColor = DeckPalette.surfaceVariant,
                focusedTextColor = DeckPalette.onBackground,
                unfocusedTextColor = DeckPalette.onBackground,
                focusedIndicatorColor = DeckPalette.primary,
                unfocusedIndicatorColor = DeckPalette.line2,
                cursorColor = DeckPalette.primary,
            ),
            modifier = Modifier
                .width(width)
                .clip(RoundedCornerShape(10.dp)),
        )
    }
}

/** A Deck-styled pill button (primary = filled blue, neutral = surface with hairline). */
@Composable
internal fun DeckButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) DeckPalette.primary else DeckPalette.surface)
            .border(
                width = 1.dp,
                color = if (primary) DeckPalette.primary else DeckPalette.line,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = text,
            color = if (primary) DeckPalette.onPrimary else DeckPalette.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
