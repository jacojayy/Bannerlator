package com.winlator.star.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.winlator.star.store.deckTileFocus
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * One page row at a time (Storage sub-tabs, container editor tabs) binds itself here, so the Deck
 * shell can drive L1/R1 even when focus sits on the side rail. Pills stay touch-only, never D-pad.
 */
internal class DeckTabHost {
    var count by mutableIntStateOf(0)
        private set
    var index by mutableIntStateOf(0)
        private set

    /** A page can claim the B button (e.g. a repository detail closes before leaving the section). */
    var onBack: (() -> Boolean)? = null

    fun bind(pages: Int) {
        count = pages
        if (index !in 0 until pages) index = 0
    }

    fun select(page: Int) {
        if (page in 0 until count) index = page
    }

    fun step(delta: Int) {
        if (count > 1) index = (index + delta + count) % count
    }
}

internal val LocalDeckTabs = compositionLocalOf { DeckTabHost() }

/** DroidDeck's own ease: quick off the mark, then a long glide — the "dragged" feel. */
internal val DeckEase = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

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
 * DroidDeck's `SettingsRow`. [configPanel] shows a small blue chevron after the control to signal
 * that a config panel is available behind it.
 */
@Composable
internal fun DeckRow(
    label: String,
    hint: String? = null,
    configPanel: Boolean = false,
    onConfigClick: (() -> Unit)? = null,
    control: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onConfigClick != null) Modifier.clickable(onClick = onConfigClick) else Modifier)
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
        if (configPanel) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Has a config panel",
                tint = DeckPalette.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DeckPalette.line),
    )
}

/**
 * A [DeckRow] whose chevron toggles an inline editor that expands BELOW it, pushing the rest of the
 * group's content down (no dialog, no navigation). The chevron flips from right-pointing to
 * down-pointing while open. Tap the header row or the chevron to toggle.
 */
@Composable
internal fun DeckExpandableRow(
    label: String,
    hint: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    control: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "deckExpandChevron")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
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
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = DeckPalette.primary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(18.dp)
                    .rotate(rotation),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DeckPalette.line),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220)) + fadeIn(tween(160)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeckPalette.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                content = content,
            )
        }
    }
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
    configPanel: Boolean = false,
    onConfigClick: (() -> Unit)? = null,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    DeckRow(
        label = label,
        hint = hint,
        configPanel = configPanel,
        onConfigClick = onConfigClick,
    ) {
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

/**
 * A Deck text field waits for A/Enter before it starts editing, so the D-pad never falls into a
 * text box (or raises the IME) while the player is just navigating. B / Back leaves the field and
 * returns the cursor to the same row.
 */
internal class DeckEditState {
    var editing by mutableStateOf(false)
    internal var returnFocus = false
    internal var trackerHeld = false
    internal val fieldFocus = FocusRequester()
}

/**
 * Lets the surrounding screen (or popup) know a field is taking input, so the B button closes the
 * keyboard before it closes the screen. One field can be editing at a time, hence a single flag.
 */
internal class DeckEditTracker {
    private var active by mutableIntStateOf(0)
    val editing: Boolean get() = active > 0
    internal fun hold() { active++ }
    internal fun release() { if (active > 0) active-- }
}

internal val LocalDeckEditTracker = compositionLocalOf { DeckEditTracker() }

@Composable
internal fun rememberDeckEditState(): DeckEditState {
    val state = remember { DeckEditState() }
    val tracker = LocalDeckEditTracker.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.editing) {
        if (state.editing && !state.trackerHeld) {
            state.trackerHeld = true
            tracker.hold()
        } else if (!state.editing && state.trackerHeld) {
            state.trackerHeld = false
            tracker.release()
        }
        if (state.editing) {
            runCatching { state.fieldFocus.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (state.trackerHeld) {
                state.trackerHeld = false
                tracker.release()
            }
        }
    }
    BackHandler(enabled = state.editing) {
        state.returnFocus = true
        state.editing = false
    }
    return state
}

/** Transparent focus target laid over a field while it is not editing. A / Enter starts editing. */
@Composable
internal fun DeckEditOverlay(state: DeckEditState, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (state.returnFocus) {
            state.returnFocus = false
            runCatching { focus.requestFocus() }
        }
    }
    Box(
        modifier = modifier
            .focusRequester(focus)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { state.editing = true }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.ButtonA || event.key == Key.Enter)
                ) {
                    state.editing = true
                    true
                } else false
            },
    )
}

/**
 * Deck's stretchy tab strip: the accent pill grows toward the tab being opened, then contracts onto
 * it (the Steam Deck / DroidDeck tab feel) instead of sliding at a fixed width. Callers drive
 * selection with L1/R1; [focusable] keeps the labels on the D-pad path for the store tab bars.
 */
@Composable
internal fun DeckStretchyTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    badges: List<Int>? = null,
    focusable: Boolean = false,
    scroll: Boolean = false,
    height: Dp = 40.dp,
) {
    val density = LocalDensity.current
    val lefts = remember(labels.size) { mutableStateListOf<Float>() }
    val rights = remember(labels.size) { mutableStateListOf<Float>() }
    val start = remember { Animatable(0f) }
    val end = remember { Animatable(0f) }
    var lastSelected by remember { mutableIntStateOf(-1) }
    val scrollState = rememberScrollState()

    val targetLeft = lefts.getOrNull(selected)
    val targetRight = rights.getOrNull(selected)
    LaunchedEffect(selected, targetLeft, targetRight) {
        val l = targetLeft ?: return@LaunchedEffect
        val r = targetRight ?: return@LaunchedEffect
        when {
            lastSelected == -1 -> {
                start.snapTo(l)
                end.snapTo(r)
                lastSelected = selected
            }
            selected == lastSelected -> {
                if (start.value != l || end.value != r) {
                    start.snapTo(l)
                    end.snapTo(r)
                }
            }
            l >= start.value -> {
                end.animateTo(r, tween(170, easing = DeckEase))
                start.animateTo(l, tween(210, easing = DeckEase))
                lastSelected = selected
            }
            else -> {
                start.animateTo(l, tween(170, easing = DeckEase))
                end.animateTo(r, tween(210, easing = DeckEase))
                lastSelected = selected
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + 6.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(DeckPalette.surfaceVariant)
            .border(1.dp, DeckPalette.line2, RoundedCornerShape(13.dp))
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (scroll) Modifier.horizontalScroll(scrollState) else Modifier),
        ) {
            Box(Modifier.height(height)) {
                if (end.value > start.value) {
                    Box(
                        Modifier
                            .offset { IntOffset(start.value.roundToInt(), 0) }
                            .width(with(density) { (end.value - start.value).toDp() })
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeckPalette.primary),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .height(height)
                        .then(if (scroll) Modifier else Modifier.fillMaxWidth()),
                ) {
                    labels.forEachIndexed { index, label ->
                        DeckStretchyTab(
                            label = label,
                            badge = badges?.getOrNull(index) ?: 0,
                            active = index == selected,
                            focusable = focusable,
                            padding = if (scroll) 16.dp else 6.dp,
                            modifier = if (scroll) Modifier else Modifier.weight(1f),
                            onClick = { onSelect(index) },
                            onMeasured = { left, right ->
                                while (lefts.size <= index) {
                                    lefts.add(0f)
                                    rights.add(0f)
                                }
                                if (lefts[index] != left || rights[index] != right) {
                                    lefts[index] = left
                                    rights[index] = right
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckStretchyTab(
    label: String,
    badge: Int,
    active: Boolean,
    focusable: Boolean,
    padding: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMeasured: (Float, Float) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val ink by animateColorAsState(
        if (active) DeckPalette.onPrimary else DeckPalette.onSurfaceVariant,
        tween(190, easing = DeckEase),
        label = "deckTabInk",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .then(if (focusable) Modifier else Modifier.focusProperties { canFocus = false })
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .onGloballyPositioned { coords ->
                val left = coords.positionInParent().x
                onMeasured(left, left + coords.size.width)
            }
            .padding(horizontal = padding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = ink,
                fontSize = 13.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
            )
            if (badge > 0) {
                Text(
                    text = " ($badge)",
                    color = ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
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
    val edit = rememberDeckEditState()
    DeckRow(label, hint) {
        Box(Modifier.width(width)) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                readOnly = !edit.editing,
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
                    .fillMaxWidth()
                    .focusProperties { canFocus = edit.editing }
                    .focusRequester(edit.fieldFocus)
                    .onFocusChanged { if (edit.editing && !it.isFocused) edit.editing = false }
                    .onPreviewKeyEvent { event ->
                        if (edit.editing && event.type == KeyEventType.KeyDown &&
                            (event.key == Key.ButtonB || event.key == Key.Back)
                        ) {
                            edit.returnFocus = true
                            edit.editing = false
                            true
                        } else false
                    }
                    .clip(RoundedCornerShape(10.dp)),
            )
            if (!edit.editing) DeckEditOverlay(edit, Modifier.matchParentSize())
        }
    }
}

/** A Deck-styled pill button (primary = filled blue, neutral = surface with hairline). */
@Composable
internal fun DeckButton(
    text: String,
    primary: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .deckTileFocus(interaction)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) DeckPalette.primary else DeckPalette.surface)
            .border(
                width = 1.dp,
                color = if (primary) DeckPalette.primary else DeckPalette.line,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
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
