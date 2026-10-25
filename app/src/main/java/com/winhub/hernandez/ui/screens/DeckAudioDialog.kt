package com.winhub.hernandez.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.winhub.hernandez.ui.components.ANDROID_OUTPUT_MS
import com.winhub.hernandez.ui.components.AUDIO_PRESETS
import com.winhub.hernandez.ui.components.AudioConfig
import com.winhub.hernandez.ui.components.PRESET_CUSTOM
import com.winhub.hernandez.ui.components.effectiveConfigFor
import com.winhub.hernandez.ui.components.latencyIsDirectBuffer
import com.winhub.hernandez.ui.components.latencyUsedBy

/**
 * Deck-native replacement for [com.winhub.hernandez.ui.components.AudioSettingsDialog]. Same [AudioConfig]
 * model, preset matrix, engine-aware latency/buffer semantics and save contract, re-dressed with Deck
 * widgets so every popup the Deck shortcut editor opens stays on-brand.
 */
@Composable
internal fun DeckAudioSettingsDialog(
    initial: AudioConfig,
    scopeLabel: String,
    latencyLive: Boolean,
    driverLabel: String = "",
    driverId: String = "",
    onDismiss: () -> Unit,
    onSave: (AudioConfig) -> Unit,
) {
    var cfg by remember { mutableStateOf(initial) }
    val custom = cfg.preset == PRESET_CUSTOM
    val directBuffer = latencyIsDirectBuffer(driverId)
    val shown = effectiveConfigFor(cfg, driverId)
    val latencyUsed = latencyUsedBy(driverId)

    DeckConfigDialog(title = "Audio settings", onDismiss = onDismiss) {
        DeckConfigNote("Balance crackle-free vs low delay  ·  $scopeLabel")
        if (driverLabel.isNotBlank()) DeckConfigNote("Engine: $driverLabel")

        DeckConfigChipGrid(
            label = "Preset",
            chips = AUDIO_PRESETS.map { Triple(it.name, cfg.preset == it.id, { cfg = it.cfg }) },
        )

        DeckGroup("Fine-tune" + if (custom) "" else " · pick Custom to edit") {
            val perfOpts = if (driverId == "directaudio")
                listOf("0" to "None", "1" to "Low lat.")
            else listOf("0" to "None", "1" to "Low lat.", "2" to "Power")
            DeckChoiceRow(
                label = "Output mode",
                hint = "AAudio performance mode",
                options = perfOpts,
                selected = shown.perfMode.toString(),
                onPick = { if (custom) cfg = cfg.copy(perfMode = it.toInt()) },
            )
            DeckRow("Adaptive buffer", "Auto-grow only if it hears crackle") {
                DeckToggle(shown.adaptive) { if (custom) cfg = cfg.copy(adaptive = it) }
            }
            if (directBuffer) {
                DeckSliderRow(
                    label = "Audio buffer",
                    hint = "≈${shown.latencyMsec + ANDROID_OUTPUT_MS} ms total · ${shown.latencyMsec} ms buffer" +
                        if (!latencyLive) "  · next launch" else "",
                    value = shown.latencyMsec.toFloat(),
                    range = 4f..120f,
                    format = { "${(it / 4).toInt() * 4} ms" },
                    onChange = { if (custom && latencyUsed) cfg = cfg.copy(latencyMsec = (it / 4).toInt() * 4) },
                )
            } else {
                DeckSliderRow(
                    label = "Guest buffer",
                    hint = (if (!latencyUsed) "not used by ALSA — set the buffer rows below"
                            else "winepulse · ${shown.latencyMsec} ms") +
                        if (!latencyLive) "  · next launch" else "",
                    value = shown.latencyMsec.toFloat(),
                    range = 20f..200f,
                    format = { "${(it / 10).toInt() * 10} ms" },
                    onChange = { if (custom && latencyUsed) cfg = cfg.copy(latencyMsec = (it / 10).toInt() * 10) },
                )
            }
            DeckRow("Initial sink buffer", "frames · 0 = auto") {
                DeckAudioStepper(if (shown.bufferFrames > 0) "${shown.bufferFrames}" else "auto", custom) {
                    cfg = cfg.copy(bufferFrames = (cfg.bufferFrames + it * 256).coerceAtLeast(0))
                }
            }
            DeckRow("Max sink buffer", "growth cap · 0 = device max") {
                DeckAudioStepper(if (shown.maxBufferFrames > 0) "${shown.maxBufferFrames}" else "device max", custom) {
                    cfg = cfg.copy(maxBufferFrames = (cfg.maxBufferFrames + it * 256).coerceAtLeast(0))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DeckButton("Cancel", primary = false, onClick = onDismiss)
            DeckButton("Save", primary = true) { onSave(cfg) }
        }
    }
}

@Composable
private fun DeckAudioStepper(value: String, enabled: Boolean, onStep: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DeckConfigChip("−", selected = false, onClick = { if (enabled) onStep(-1) })
        Text(
            text = value,
            color = DeckPalette.primary,
            fontSize = 13.sp,
            modifier = Modifier.widthIn(min = 72.dp).padding(horizontal = 6.dp),
        )
        DeckConfigChip("+", selected = false, onClick = { if (enabled) onStep(1) })
    }
}
