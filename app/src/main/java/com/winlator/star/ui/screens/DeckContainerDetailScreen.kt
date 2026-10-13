package com.winlator.star.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.FastOutLinearInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusGroup
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winlator.star.container.Container
import com.winlator.star.store.deckTileFocus
import java.util.Locale

/**
 * Deck Mode's container editor — the classic editor's SAME settings, regrouped into modern
 * top tabs (General / Graphics / Performance / CPU / Input / Environment / Wine). Tab content
 * slides rapidly bottom-to-top on every switch (DroidDeck-style). Controller-first: focus lands
 * on the first tab pill (never the name text field, which swallowed D-pad input), so the whole
 * screen is D-pad navigable — tabs, rows, Cancel / Save.
 *
 * It binds to the SAME [ContainerDetailViewModel] the classic editor uses — so all load/save,
 * preset, and Wayland-compositor logic is shared verbatim. The seven `resolved*` params that the
 * classic screen pulls from AndroidView-backed refs (CPUListView, ColorPickerView, KeyValueSet tags)
 * are synthesized straight from the view-model's own state, which is exactly the fallback the
 * classic screen already uses when those refs are absent (`?: viewModel.cpuList`, `?: "#0277bd"`).
 */
@Composable
internal fun DeckContainerDetailScreen(
    containerId: Int,
    onNavigateBack: () -> Unit,
    viewModel: ContainerDetailViewModel = viewModel(),
) {
    LaunchedEffect(containerId) { viewModel.init(containerId) }

    val title = when {
        viewModel.defaultsMode -> "New Container Defaults"
        containerId <= 0 -> "New Container"
        else -> viewModel.containerName.ifBlank { "Container" }
    }
    val saveLabel = when {
        viewModel.isSaving -> "Saving…"
        viewModel.isEditMode -> "Save Changes"
        else -> "Create Container"
    }

    val tabTitles = remember {
        listOf("General", "Graphics", "Performance", "CPU", "Input", "Environment", "Wine")
    }
    var tab by remember { mutableStateOf(0) }
    val firstTabFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstTabFocus.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background),
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Deck.onBackground)
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Deck.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                Text(
                    text = when {
                        viewModel.defaultsMode -> "Saved as the profile every NEW container starts from"
                        viewModel.isEditMode -> "Editing an existing container"
                        else -> "Configure a fresh container, then create it"
                    },
                    color = Deck.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Text(
                text = when {
                    viewModel.defaultsMode -> "DEFAULTS"
                    viewModel.isEditMode -> "EDIT"
                    else -> "NEW"
                },
                color = Deck.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
        }

        // ── Top tab pills (controller-navigable; focus starts on the first) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState())
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabTitles.forEachIndexed { i, tabTitle ->
                val active = tab == i
                val interaction = remember { MutableInteractionSource() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .deckTileFocus(interaction)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) DeckPalette.primary else DeckPalette.surface)
                        .border(
                            1.dp,
                            if (active) DeckPalette.primary else DeckPalette.line,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(interactionSource = interaction, indication = null) { tab = i }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .then(if (i == 0) Modifier.focusRequester(firstTabFocus) else Modifier),
                ) {
                    Text(
                        tabTitle,
                        color = if (active) DeckPalette.onPrimary else DeckPalette.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }

        // ── Tab content — rapid bottom-to-top slide on every switch ──────────
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (slideInVertically(tween(240, easing = FastOutLinearInEasing)) { it } + fadeIn(tween(160)))
                    .togetherWith(
                        slideOutVertically(tween(140)) { -it / 4 } + fadeOut(tween(100)),
                    )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusGroup(),
            label = "deckContainerTab",
        ) { currentTab ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                when (currentTab) {
                    0 -> DeckGroup("General") {
                        DeckTextRow(
                            label = "Container Name",
                            hint = "Shown in the container list",
                            value = viewModel.containerName,
                            onValueChange = { viewModel.containerName = it },
                            width = 360.dp,
                        )
                        DeckChoiceRow(
                            label = "Screen Size",
                            options = viewModel.screenSizeEntries.map { it to it },
                            selected = viewModel.selectedScreenSize,
                            onPick = { viewModel.selectedScreenSize = it },
                        )
                        DeckChoiceRow(
                            label = "Wine Version",
                            hint = "The Wine / Proton layer this container runs on",
                            options = viewModel.wineVersionEntries.map { it to it },
                            selected = viewModel.selectedWineVersion,
                            onPick = { viewModel.selectedWineVersion = it },
                        )
                        DeckChoiceRow(
                            label = "Audio Driver",
                            options = viewModel.audioDriverEntries.map { it to it },
                            selected = viewModel.selectedAudioDriver,
                            onPick = { viewModel.selectedAudioDriver = it },
                        )
                        DeckChoiceRow(
                            label = "Language",
                            options = viewModel.lcAllEntries.map { it to it },
                            selected = viewModel.lcAll,
                            onPick = { viewModel.lcAll = it },
                        )
                        DeckRow(
                            label = "Run as Administrator",
                            hint = "Disables UAC (EnableLUA=0) inside the Wine prefix",
                            control = { DeckToggle(viewModel.runAsAdmin) { viewModel.runAsAdmin = it } },
                        )
                    }

                    1 -> DeckGroup("Graphics") {
                        DeckChoiceRow(
                            label = "Graphics Driver",
                            hint = "Turnip / system Vulkan driver for Direct3D",
                            options = viewModel.graphicsDriverEntries.map { it to it },
                            selected = viewModel.selectedGraphicsDriver,
                            onPick = { viewModel.selectedGraphicsDriver = it },
                        )
                        DeckChoiceRow(
                            label = "DX Wrapper",
                            options = viewModel.dxWrapperEntries.map { it to it },
                            selected = viewModel.selectedDXWrapper,
                            onPick = { viewModel.selectedDXWrapper = it },
                        )
                        DeckChoiceRow(
                            label = "Renderer",
                            options = viewModel.rendererEntries.map { it to it },
                            selected = viewModel.selectedRenderer,
                            onPick = { viewModel.selectedRenderer = it },
                        )
                        DeckChoiceRow(
                            label = "Display Backend",
                            hint = "Wayland routes launches through the embedded compositor",
                            options = listOf(
                                "X11" to Container.DISPLAY_BACKEND_X11,
                                "Wayland" to Container.DISPLAY_BACKEND_WAYLAND,
                            ),
                            selected = viewModel.displayBackend,
                            onPick = { viewModel.displayBackend = it },
                        )
                        DeckChoiceRow(
                            label = "Fullscreen",
                            options = listOf(
                                "Windowed" to Container.FULLSCREEN_OFF.toString(),
                                "Fit (letterboxed)" to Container.FULLSCREEN_FIT.toString(),
                                "Stretch" to Container.FULLSCREEN_STRETCH.toString(),
                            ),
                            selected = viewModel.fullscreenMode.toString(),
                            onPick = { viewModel.fullscreenMode = it.toInt() },
                        )
                        DeckRow(
                            label = "Show FPS",
                            control = { DeckToggle(viewModel.showFPS) { viewModel.showFPS = it } },
                        )
                        DeckRow(
                            label = "Auto Close on Exit",
                            control = { DeckToggle(viewModel.autoCloseOnExit) { viewModel.autoCloseOnExit = it } },
                        )
                    }

                    2 -> DeckGroup("Performance") {
                        DeckChoiceRow(
                            label = "Frame Generation",
                            hint = "Bionic-FG or LSFG frame interpolation",
                            options = listOf(
                                "Off" to "off",
                                "Bionic-FG" to "bionic",
                                "LSFG" to "lsfg",
                            ),
                            selected = viewModel.frameGenEngine,
                            onPick = { viewModel.frameGenEngine = it },
                        )
                        DeckRow(
                            label = "FPS Limiter",
                            hint = "Loads the limiter layer; the cap is tuned in-game",
                            control = { DeckToggle(viewModel.fpsLimiterEnabled) { viewModel.fpsLimiterEnabled = it } },
                        )
                        DeckRow(
                            label = "Match Refresh Rate",
                            hint = "Sync the panel refresh to the game's FPS",
                            control = { DeckToggle(viewModel.matchRefreshRate) { viewModel.matchRefreshRate = it } },
                        )
                        DeckRow(
                            label = "LSFG Performance Mode",
                            control = { DeckToggle(viewModel.lsfgPerformanceMode) { viewModel.lsfgPerformanceMode = it } },
                        )
                        DeckRow(
                            label = "LSFG Auto Enable",
                            hint = "Start frame generation live at launch",
                            control = { DeckToggle(viewModel.lsfgAutoEnable) { viewModel.lsfgAutoEnable = it } },
                        )
                    }

                    3 -> DeckGroup("CPU & Emulation") {
                        if (viewModel.emulatorEnabled) {
                            DeckChoiceRow(
                                label = "Emulator",
                                options = viewModel.emulatorEntries.map { it to it },
                                selected = viewModel.selectedEmulator,
                                onPick = { viewModel.selectedEmulator = it },
                            )
                        }
                        DeckChoiceRow(
                            label = "Box64 Version",
                            options = viewModel.box64VersionEntries.map { it to it },
                            selected = viewModel.selectedBox64Version,
                            onPick = { viewModel.selectedBox64Version = it },
                        )
                        DeckChoiceRow(
                            label = "Box64 Preset",
                            options = viewModel.box64PresetEntries.mapIndexed { i, e -> e to i.toString() },
                            selected = viewModel.selectedBox64PresetIndex.toString(),
                            onPick = { viewModel.selectedBox64PresetIndex = it.toInt() },
                        )
                        if (viewModel.isArm64EC) {
                            DeckChoiceRow(
                                label = "FEXCore Version",
                                options = viewModel.fexCoreVersionEntries.map { it to it },
                                selected = viewModel.selectedFEXCoreVersion,
                                onPick = { viewModel.selectedFEXCoreVersion = it },
                            )
                            DeckChoiceRow(
                                label = "FEXCore Preset",
                                options = viewModel.fexCorePresetEntries.mapIndexed { i, e -> e to i.toString() },
                                selected = viewModel.selectedFEXCorePresetIndex.toString(),
                                onPick = { viewModel.selectedFEXCorePresetIndex = it.toInt() },
                            )
                        }
                    }

                    4 -> DeckGroup("Input") {
                        DeckRow(
                            label = "XInput",
                            control = { DeckToggle(viewModel.enableXInput) { viewModel.enableXInput = it } },
                        )
                        DeckRow(
                            label = "DInput",
                            control = { DeckToggle(viewModel.enableDInput) { viewModel.enableDInput = it } },
                        )
                        DeckChoiceRow(
                            label = "Controller Vibration",
                            options = listOf(
                                "Off" to Container.VIBRATION_MODE_OFF.toString(),
                                "Controller" to Container.VIBRATION_MODE_CONTROLLER.toString(),
                                "Device" to Container.VIBRATION_MODE_DEVICE.toString(),
                                "Both" to Container.VIBRATION_MODE_BOTH.toString(),
                            ),
                            selected = viewModel.vibrationMode.toString(),
                            onPick = { viewModel.vibrationMode = it.toInt() },
                        )
                        DeckSliderRow(
                            label = "Vibration Intensity",
                            value = viewModel.vibrationIntensity.toFloat(),
                            range = 0f..100f,
                            format = { "${it.toInt()}%" },
                            onChange = { viewModel.vibrationIntensity = it.toInt() },
                        )
                        DeckRow(
                            label = "Gyroscope Aim",
                            hint = "Motion-controlled aiming default for this container",
                            control = { DeckToggle(viewModel.gyroEnabled) { viewModel.gyroEnabled = it } },
                        )
                    }

                    5 -> DeckGroup("Environment") {
                        DeckTextRow(
                            label = "Env Vars",
                            hint = "KEY=value pairs, one per line",
                            value = viewModel.envVarsStr,
                            onValueChange = { viewModel.envVarsStr = it },
                            width = 480.dp,
                        )
                        DeckChoiceRow(
                            label = "Startup Selection",
                            options = viewModel.startupSelectionEntries.mapIndexed { i, e -> e to i.toString() },
                            selected = viewModel.selectedStartupSelection.toString(),
                            onPick = { viewModel.selectedStartupSelection = it.toInt() },
                        )
                    }

                    else -> DeckGroup("Wine") {
                        DeckChoiceRow(
                            label = "Desktop Theme",
                            options = listOf("Light" to "0", "Dark" to "1"),
                            selected = viewModel.desktopThemeIndex.toString(),
                            onPick = { viewModel.desktopThemeIndex = it.toInt() },
                        )
                        DeckChoiceRow(
                            label = "Mouse Warp",
                            options = viewModel.mouseWarpEntries.mapIndexed { i, e -> e to i.toString() },
                            selected = viewModel.selectedMouseWarpIndex.toString(),
                            onPick = { viewModel.selectedMouseWarpIndex = it.toInt() },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Save bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeckButton("Cancel", primary = false) { onNavigateBack() }
            Spacer(Modifier.weight(1f))
            DeckButton(saveLabel, primary = true) {
                if (!viewModel.isSaving) viewModel.confirm(
                    resolvedGraphicsDriverConfig = viewModel.graphicsDriverConfig,
                    resolvedDXWrapperConfig      = viewModel.dxWrapperConfig,
                    resolvedFPSCounterConfig     = viewModel.fpsCounterConfig,
                    resolvedEnvVars              = viewModel.envVarsStr,
                    resolvedCPUList              = viewModel.cpuList,
                    resolvedCPUListWoW64         = viewModel.cpuListWoW64,
                    resolvedColorAsString        = deckColorAsString(viewModel.desktopBgColorInt),
                    onDone                       = onNavigateBack,
                )
            }
        }
    }
}

/** androidx Color int (ARGB) -> the "#RRGGBB" string the classic ColorPickerView exposed. */
private fun deckColorAsString(argb: Int): String =
    String.format(Locale.US, "#%06X", 0xFFFFFF and argb)
