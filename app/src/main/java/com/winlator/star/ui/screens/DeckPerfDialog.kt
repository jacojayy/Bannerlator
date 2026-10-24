package com.winlator.star.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.winlator.star.perf.PerfRootApplier
import com.winlator.star.perf.PerformanceSettings
import com.winlator.star.ui.rememberLiveReadouts

/**
 * Deck-native replacement for [com.winlator.star.ui.PerformanceDashboardDialogPerGame]. Same state
 * surface (per-game overrides + root presets + profiles + reset), re-dressed with Deck widgets so the
 * Deck shortcut editor never falls back to classic Material for its popups.
 */
@Composable
internal fun DeckPerformanceDialog(
    sustained: Boolean, onSustained: (Boolean) -> Unit,
    priority: Boolean, onPriority: (Boolean) -> Unit,
    bigCores: Boolean, onBigCores: (Boolean) -> Unit,
    rootValue: (String) -> Boolean, onRoot: (String, Boolean) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val readouts = rememberLiveReadouts()
    var profile by remember { mutableStateOf<String?>(null) }
    var pendingDanger by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    fun globalOf(key: String): Boolean = when (key) {
        "sustainedPerfMode" -> PerformanceSettings.sustainedPerfMode.value
        "perfPriorityBoost" -> PerformanceSettings.perfPriorityBoost.value
        "preferBigCores" -> PerformanceSettings.preferBigCores.value
        else -> PerformanceSettings.rootDefaultValue(key)
    }

    fun applyProfile(p: String) {
        profile = p
        when (p) {
            "Off", "Battery" -> {
                onSustained(false); onPriority(false); onBigCores(false)
                onRoot(PerfRootApplier.KEY_GPU_CLOCK_LOCK, false)
                onRoot(PerfRootApplier.KEY_CPU_GOVERNOR, false)
                onRoot(PerfRootApplier.KEY_CPU_FREQ_LOCK, false)
                onRoot(PerfRootApplier.KEY_CORES_ONLINE, false)
            }
            "Balanced" -> {
                onSustained(true); onPriority(false); onBigCores(false)
                onRoot(PerfRootApplier.KEY_GPU_CLOCK_LOCK, false)
            }
            else -> {
                onSustained(true); onPriority(true); onBigCores(true)
                onRoot(PerfRootApplier.KEY_GPU_CLOCK_LOCK, true)
                onRoot(PerfRootApplier.KEY_CPU_GOVERNOR, true)
                onRoot(PerfRootApplier.KEY_CPU_FREQ_LOCK, true)
                onRoot(PerfRootApplier.KEY_CORES_ONLINE, true)
            }
        }
    }

    DeckConfigDialog(title = "Performance", onDismiss = onDismiss) {
        DeckConfigNote("Per-game preset · saved on OK · honored at launch")

        DeckConfigPanel("Device") {
            DeckRow("GPU clock", readouts["gpuMhz"]?.let { "$it MHz" } ?: "—") {}
            DeckRow("SoC temp", readouts["socTemp"]?.let { "$it °C" } ?: "—") {}
            DeckRow("Governor", readouts["governor"] ?: "—") {}
        }

        DeckConfigChipGrid(
            label = "Power profile",
            chips = listOf("Off", "Battery", "Balanced", "Performance", "Turbo")
                .map { Triple(it, profile == it, { applyProfile(it) }) },
        )

        DeckGroup("Quick controls · no root") {
            DeckRow("Sustained Performance Mode", "Holds a stable clock to avoid thermal throttling.") {
                DeckToggle(sustained, onSustained)
            }
            DeckRow("Thread Priority Boost", "Raises emulator thread priority so the scheduler favours the game.") {
                DeckToggle(priority, onPriority)
            }
            DeckRow("Prefer Big Cores", "Routes emulator threads onto the big / prime cluster.") {
                DeckToggle(bigCores, onBigCores)
            }
            DeckRow("Lock GPU to max clock", "Adreno: no-root KGSL turbo; sysfs pin with root.") {
                val k = PerfRootApplier.KEY_GPU_CLOCK_LOCK
                DeckToggle(rootValue(k)) { onRoot(k, it) }
            }
        }

        DeckGroup("Root & danger") {
            DeckConfigNote("Saved for this game · applied at launch when root is available.")
            DeckRow("CPU governor → performance", "Forces every cluster to the performance governor.") {
                val k = PerfRootApplier.KEY_CPU_GOVERNOR
                DeckToggle(rootValue(k)) { onRoot(k, it) }
            }
            DeckRow("Lock CPU frequency to max", "scaling_min_freq = scaling_max_freq on every cluster.") {
                val k = PerfRootApplier.KEY_CPU_FREQ_LOCK
                DeckToggle(rootValue(k)) { onRoot(k, it) }
            }
            DeckRow("Keep all cores online", "Prevents the kernel from hot-plugging cores offline.") {
                val k = PerfRootApplier.KEY_CORES_ONLINE
                DeckToggle(rootValue(k)) { onRoot(k, it) }
            }
            DeckRow("Disable thermal throttling", "Runtime watchdog still guards.") {
                val k = PerfRootApplier.KEY_THERMAL_DISABLE
                DeckToggle(rootValue(k)) { on ->
                    if (on) pendingDanger = "Disable thermal throttling" to { onRoot(k, true) } else onRoot(k, false)
                }
            }
            DeckRow("Fan to maximum", "Forces active cooling to full at launch.") {
                val k = PerfRootApplier.KEY_FAN_MAX
                DeckToggle(rootValue(k)) { on ->
                    if (on) pendingDanger = "Fan to maximum" to { onRoot(k, true) } else onRoot(k, false)
                }
            }
        }

        val anyOverride = sustained != globalOf("sustainedPerfMode") ||
            priority != globalOf("perfPriorityBoost") ||
            bigCores != globalOf("preferBigCores") ||
            PerfRootApplier.ROOT_KEYS.any { rootValue(it) != globalOf(it) }
        if (anyOverride) {
            DeckButton(text = "Reset all to global", primary = false, onClick = onResetAll)
        }
    }

    pendingDanger?.let { pd ->
        PerfDisclaimerDialog(
            title = pd.first,
            body = PerfDisclaimerCopy.ROOT_RISK,
            confirmLabel = "Enable",
            onConfirm = { pd.second(); pendingDanger = null },
            onDismiss = { pendingDanger = null },
        )
    }
}
