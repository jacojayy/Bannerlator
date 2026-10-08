package com.winlator.star.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.winlator.star.R
import com.winlator.star.contents.ContentsManager
import com.winlator.star.core.AppOrientation
import com.winlator.star.core.UpdateManager
import com.winlator.star.core.WinFgCapture
import com.winlator.star.core.WinFgDiag
import com.winlator.star.box64.Box64Preset
import com.winlator.star.box64.Box64PresetManager
import com.winlator.star.fexcore.FEXCorePreset
import com.winlator.star.fexcore.FEXCorePresetManager
import com.winlator.star.store.SteamPrefs
import com.winlator.star.store.SteamRegion

/**
 * Deck Mode's settings page — a 1:1 mirror of [SettingsScreen]: the same preference keys, the same
 * defaults, the same choice lists (incl. dynamic Box64/FEXCore presets), and the same save semantics
 * (immediate-write vs. batched FAB commit), rendered in DroidDeck's group/row/chip language.
 */
@Composable
internal fun DeckSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // ── Dynamic preset lists (same source as SettingsScreen: the preset managers). ──
    var box64Presets by remember { mutableStateOf(listOf<Box64Preset>()) }
    var fexPresets by remember { mutableStateOf(listOf<FEXCorePreset>()) }
    LaunchedEffect(Unit) {
        box64Presets = Box64PresetManager.getPresets("box64", context)
        fexPresets = FEXCorePresetManager.getPresets(context)
    }

    // ── Batched (FAB-saved) state — mirrors SettingsScreen's saveSettings() set. ──
    var box64 by remember { mutableStateOf(prefs.getString("box64_preset", "COMPATIBILITY") ?: "COMPATIBILITY") }
    var fex by remember { mutableStateOf(prefs.getString("fexcore_preset", "COMPATIBILITY") ?: "COMPATIBILITY") }
    var darkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", false)) }
    var bigPicture by remember { mutableStateOf(prefs.getBoolean("enable_big_picture_mode", false)) }
    var landing by remember { mutableStateOf(prefs.getString("default_landing_screen", "games") ?: "games") }
    var apiKeyEnabled by remember { mutableStateOf(prefs.getBoolean("enable_custom_api_key", false)) }
    var apiKey by remember { mutableStateOf(prefs.getString("custom_api_key", "") ?: "") }
    var cursorLock by remember { mutableStateOf(prefs.getBoolean("cursor_lock", false)) }
    var xinputOff by remember { mutableStateOf(prefs.getBoolean("xinput_toggle", false)) }
    var dri3 by remember { mutableStateOf(prefs.getBoolean("use_dri3", true)) }
    var xr by remember { mutableStateOf(prefs.getBoolean("use_xr", true)) }
    var cursorSpeed by remember { mutableStateOf(prefs.getFloat("cursor_speed", 1.0f)) }
    var fileProvider by remember { mutableStateOf(prefs.getBoolean("enable_file_provider", true)) }
    var androidBrowser by remember { mutableStateOf(prefs.getBoolean("open_with_android_browser", false)) }
    var shareClipboard by remember { mutableStateOf(prefs.getBoolean("share_android_clipboard", false)) }
    var contentsUrl by remember {
        mutableStateOf(
            prefs.getString("downloadable_contents_url", ContentsManager.REMOTE_PROFILES)
                ?: ContentsManager.REMOTE_PROFILES,
        )
    }
    var winlatorPathUri by remember { mutableStateOf<Uri?>(null) }
    var shortcutsPathUri by remember { mutableStateOf<Uri?>(null) }
    var saved by remember { mutableStateOf(false) }

    // ── Immediate-write state (SettingsScreen writes these on change, not on FAB). ──
    var notifyUpdates by remember { mutableStateOf(UpdateManager.isNotifyEnabled(context)) }
    var includePre by remember { mutableStateOf(UpdateManager.isIncludePrereleases(context)) }
    var steamChat by remember { mutableStateOf(SteamPrefs.isChatNotificationsEnabled(context)) }
    var steamOffline by remember { mutableStateOf(SteamPrefs.isOfflinePresenceEnabled(context)) }
    var steamRegion by remember { mutableStateOf(SteamRegion.mode(context)) }
    var showStores by remember { mutableStateOf(prefs.getBoolean("show_stores", true)) }
    var showInternal by remember { mutableStateOf(prefs.getBoolean("show_internal_storage", true)) }
    var showSd by remember { mutableStateOf(prefs.getBoolean("show_sd_storage", true)) }
    var orientation by remember { mutableStateOf(AppOrientation.mode(context)) }
    var uiScale by remember { mutableStateOf(prefs.getFloat("ui_scale", 0.9f).coerceIn(0.5f, 1.5f)) }
    var fontScale by remember { mutableStateOf(prefs.getFloat("font_scale", 0.9f).coerceIn(0.5f, 1.5f)) }
    var captureEnabled by remember { mutableStateOf(WinFgCapture.isEnabled(context)) }
    var captureRes by remember { mutableStateOf(WinFgCapture.captureRes(context)) }
    var captureLogging by remember { mutableStateOf(WinFgDiag.isExtraLoggingEnabled(context)) }

    // ── Overlay dialogs. ──
    var showLogManager by remember { mutableStateOf(false) }
    var showPerformance by remember { mutableStateOf(false) }
    var showCaptureConsent by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateNote by remember { mutableStateOf<String?>(null) }

    // SAF launchers for the two path pickers.
    val winlatorPathLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) winlatorPathUri = uri }
    val shortcutsPathLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) shortcutsPathUri = uri }

    fun save() {
        // Same writes as SettingsScreen.saveSettings() — only the keys this page owns, commit().
        val editor = prefs.edit()
            .putString("box64_preset", box64)
            .putString("fexcore_preset", fex)
            .putBoolean("dark_mode", darkMode)
            .putBoolean("enable_big_picture_mode", bigPicture)
            .putString("default_landing_screen", landing)
            .putBoolean("enable_custom_api_key", apiKeyEnabled)
            .putBoolean("cursor_lock", cursorLock)
            .putBoolean("xinput_toggle", xinputOff)
            .putBoolean("use_dri3", dri3)
            .putBoolean("use_xr", xr)
            .putFloat("cursor_speed", cursorSpeed)
            .putBoolean("enable_file_provider", fileProvider)
            .putBoolean("open_with_android_browser", androidBrowser)
            .putBoolean("share_android_clipboard", shareClipboard)
            .putString("downloadable_contents_url", contentsUrl)
        if (apiKeyEnabled) editor.putString("custom_api_key", apiKey)
        else editor.remove("custom_api_key")
        winlatorPathUri?.let {
            editor.putString("winlator_path_uri", it.toString())
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        shortcutsPathUri?.let {
            editor.putString("shortcuts_export_path_uri", it.toString())
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        editor.commit()
        saved = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckPalette.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DeckPalette.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Identical to the Settings tab",
            fontSize = 13.sp,
            color = DeckPalette.onSurfaceVariant,
        )

        // ── Updates ──
        DeckGroup(title = "Updates") {
            DeckRow(label = "Notify me about updates") {
                DeckToggle(notifyUpdates) {
                    notifyUpdates = it
                    UpdateManager.setNotifyEnabled(context, it)
                }
            }
            DeckRow(label = "Include pre-releases (beta builds)") {
                DeckToggle(includePre) {
                    includePre = it
                    UpdateManager.setIncludePrereleases(context, it)
                }
            }
            DeckRow(
                label = if (checkingUpdate) "Checking…" else "Check for updates",
                hint = updateNote,
            ) {
                DeckButton("Check", primary = false) {
                    checkingUpdate = true
                    updateNote = null
                    UpdateManager.check(context) { info ->
                        checkingUpdate = false
                        updateNote = info?.versionName?.let { "V $it available" } ?: "You're on the latest build"
                    }
                }
            }
        }

        // ── Steam ──
        DeckGroup(title = "Steam") {
            DeckRow(label = "Steam chat notifications") {
                DeckToggle(steamChat) {
                    steamChat = it
                    SteamPrefs.setChatNotificationsEnabled(context, it)
                }
            }
            DeckRow(label = "Show me as in-game for offline launches") {
                DeckToggle(steamOffline) {
                    steamOffline = it
                    SteamPrefs.setOfflinePresenceEnabled(context, it)
                }
            }
            DeckChoiceRow(
                label = "Steam connection region",
                options = buildList {
                    add(SteamRegion.AUTO to "Auto (nearest by ping)")
                    SteamRegion.CATALOG.forEach { add(it.code to "${it.name}  (${it.code})") }
                },
                selected = steamRegion,
                onPick = {
                    steamRegion = it
                    SteamRegion.setMode(context, it)
                },
            )
        }

        // ── Box64 ──
        DeckGroup(title = "Box64") {
            DeckChoiceRow(
                label = "Box64 Preset",
                options = box64Presets.map { it.id to it.name },
                selected = box64,
                onPick = { box64 = it; saved = false },
            )
        }

        // ── FEXCore Config ──
        DeckGroup(title = "FEXCore Config") {
            DeckChoiceRow(
                label = "FEXCore Preset",
                options = fexPresets.map { it.id to it.name },
                selected = fex,
                onPick = { fex = it; saved = false },
            )
        }

        // ── Path Settings ──
        DeckGroup(title = "Path Settings") {
            DeckRow(
                label = "Winlator Path",
                hint = winlatorPathUri?.lastPathSegment ?: "/storage/emulated/0/Winlator",
            ) {
                DeckButton("Choose", primary = false) { winlatorPathLauncher.launch(null) }
            }
            DeckRow(
                label = "Shortcut Export Path",
                hint = shortcutsPathUri?.lastPathSegment ?: "/storage/emulated/0/Winlator/Shortcuts",
            ) {
                DeckButton("Choose", primary = false) { shortcutsPathLauncher.launch(null) }
            }
        }

        // ── Side Menu ──
        DeckGroup(title = "Side Menu") {
            DeckRow(label = "Show game stores") {
                DeckToggle(showStores) {
                    showStores = it
                    prefs.edit().putBoolean("show_stores", it).apply()
                }
            }
            DeckRow(label = "Show internal storage") {
                DeckToggle(showInternal) {
                    showInternal = it
                    prefs.edit().putBoolean("show_internal_storage", it).apply()
                }
            }
            DeckRow(label = "Show SD card storage") {
                DeckToggle(showSd) {
                    showSd = it
                    prefs.edit().putBoolean("show_sd_storage", it).apply()
                }
            }
        }

        // ── App Orientation ──
        DeckGroup(title = "App Orientation") {
            DeckChoiceRow(
                label = "App Orientation",
                options = listOf(
                    AppOrientation.AUTO to "Auto",
                    AppOrientation.PORTRAIT to "Portrait",
                    AppOrientation.LANDSCAPE to "Landscape",
                ),
                selected = orientation,
                onPick = {
                    orientation = it
                    AppOrientation.setMode(context, it)
                },
            )
        }

        // ── Interface Scale ──
        DeckGroup(title = "Interface Scale") {
            DeckSliderRow(
                label = "UI Scale",
                value = uiScale,
                range = 0.5f..1.5f,
                format = { "${(it * 100).toInt()}%" },
                onChange = {
                    uiScale = it
                    prefs.edit().putFloat("ui_scale", it).apply()
                },
            )
            DeckSliderRow(
                label = "Font Size",
                value = fontScale,
                range = 0.5f..1.5f,
                format = { "${(it * 100).toInt()}%" },
                onChange = {
                    fontScale = it
                    prefs.edit().putFloat("font_scale", it).apply()
                },
            )
        }

        // ── Default Screen on Launch ──
        DeckGroup(title = "Default Screen on Launch") {
            DeckChoiceRow(
                label = "Default landing screen",
                options = listOf(
                    "games" to "Game Shortcuts",
                    "containers" to "Containers",
                ),
                selected = landing,
                onPick = { landing = it; saved = false },
            )
        }

        // ── Big Picture Mode ──
        DeckGroup(title = "Big Picture Mode") {
            DeckRow(label = "Enable Big Picture Mode on App Launch") {
                DeckToggle(bigPicture) { bigPicture = it; saved = false }
            }
        }

        // ── SteamGrid API ──
        DeckGroup(title = "SteamGrid API") {
            DeckRow(label = "Set SteamGrid API Key? (Cover Art)") {
                DeckToggle(apiKeyEnabled) { apiKeyEnabled = it; saved = false }
            }
            if (apiKeyEnabled) {
                DeckTextRow(
                    label = "API Key",
                    value = apiKey,
                    onValueChange = { apiKey = it; saved = false },
                    width = 260.dp,
                )
            }
        }

        // ── XServer ──
        DeckGroup(title = "XServer") {
            DeckSliderRow(
                label = "Cursor Speed",
                value = cursorSpeed,
                range = 0.1f..2.0f,
                format = { "${(it * 100).toInt()}%" },
                onChange = { cursorSpeed = it; saved = false },
            )
            DeckRow(label = "True Mouse Control (Deactivate with Volume Down)") {
                DeckToggle(cursorLock) { cursorLock = it; saved = false }
            }
            DeckRow(label = "Disable Xinput (Used for Exclusive M/KB support)") {
                DeckToggle(xinputOff) { xinputOff = it; saved = false }
            }
            DeckRow(label = "Use DRI3 Extension") {
                DeckToggle(dri3) { dri3 = it; saved = false }
            }
            DeckRow(label = "Use XR") {
                DeckToggle(xr) { xr = it; saved = false }
            }
        }

        // ── Logs ──
        DeckGroup(title = "Logs") {
            DeckRow(label = "Open Log Manager", hint = "Wine / Box64 logging and log location") {
                DeckButton("Open", primary = false) { showLogManager = true }
            }
        }

        // ── Experimental ──
        DeckGroup(title = "Experimental") {
            DeckRow(label = "Enable File Provider") {
                DeckToggle(fileProvider) { fileProvider = it; saved = false }
            }
            DeckRow(label = "Open with Android Browser") {
                DeckToggle(androidBrowser) { androidBrowser = it; saved = false }
            }
            DeckRow(label = "Share Android Clipboard") {
                DeckToggle(shareClipboard) { shareClipboard = it; saved = false }
            }
            DeckTextRow(
                label = "Downloadable Contents URL",
                value = contentsUrl,
                onValueChange = { contentsUrl = it; saved = false },
                width = 320.dp,
            )
        }

        // ── Developer — Frame-gen training capture ──
        DeckGroup(title = "Developer — Frame-gen training capture") {
            DeckRow(label = "Contribute frame-gen training capture") {
                DeckToggle(captureEnabled) { enable ->
                    if (enable) {
                        showCaptureConsent = true
                    } else {
                        WinFgCapture.disable(context)
                        captureEnabled = false
                    }
                }
            }
            DeckChoiceRow(
                label = "Capture resolution",
                options = listOf(
                    WinFgCapture.RES_MATCH to "Match game (native)",
                    WinFgCapture.RES_720P to "720p (1280×720)",
                    WinFgCapture.RES_1080P to "1080p (1920×1080)",
                ),
                selected = captureRes,
                onPick = {
                    captureRes = it
                    WinFgCapture.setCaptureRes(context, it)
                },
            )
            DeckRow(label = "Extra win-fg logging (verbose)") {
                DeckToggle(captureLogging) {
                    captureLogging = it
                    WinFgDiag.setExtraLoggingEnabled(context, it)
                }
            }
        }

        // ── Performance ──
        DeckGroup(title = "Performance") {
            DeckRow(label = "Open Performance settings") {
                DeckButton("Open", primary = false) { showPerformance = true }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeckPalette.primary)
                    .clickable { save() }
                    .padding(horizontal = 28.dp),
            ) {
                Text(
                    text = "Save",
                    color = DeckPalette.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (saved) {
                Text(
                    text = "✓ Saved",
                    color = DeckPalette.good,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    // ── Overlay dialogs (same as SettingsScreen). ──
    if (showLogManager) {
        AlertDialog(
            onDismissRequest = { showLogManager = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLogManager = false }) { Text("Close") }
            },
            text = { LogManagerScreen(onClose = { showLogManager = false }) },
        )
    }
    if (showPerformance) {
        AlertDialog(
            onDismissRequest = { showPerformance = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPerformance = false }) { Text("Close") }
            },
            text = { PerformanceSettingsScreen(onClose = { showPerformance = false }) },
        )
    }
    if (showCaptureConsent) {
        var consentChecked by remember { mutableStateOf(false) }
        val consentText = remember { context.getString(R.string.winfg_capture_consent_v1) }
        AlertDialog(
            onDismissRequest = { showCaptureConsent = false },
            title = { Text(context.getString(R.string.winfg_capture_consent_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(consentText, color = DeckPalette.onBackground, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = consentChecked, onCheckedChange = { consentChecked = it })
                        Text("I understand", color = DeckPalette.onBackground, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = consentChecked,
                    onClick = {
                        WinFgCapture.recordConsentAndEnable(context)
                        captureEnabled = true
                        showCaptureConsent = false
                    },
                ) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showCaptureConsent = false }) { Text("Cancel") }
            },
        )
    }
}
