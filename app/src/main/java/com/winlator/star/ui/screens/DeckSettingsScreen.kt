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
import com.winlator.star.ui.theme.AppThemeState
import android.app.Activity
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.util.concurrent.Executors
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import com.winlator.star.MainActivity
import com.winlator.star.util.InAppFilePicker
import com.winlator.star.contentdialog.ContentDialog
import com.winlator.star.core.AppUtils
import com.winlator.star.core.PreloaderDialog
import com.winlator.star.midi.MidiManager
import com.winlator.star.xenvironment.ImageFsInstaller

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
    var showStores by remember { mutableStateOf(AppThemeState.showStores.value) }
    var showInternal by remember { mutableStateOf(AppThemeState.showInternalStorage.value) }
    var showSd by remember { mutableStateOf(AppThemeState.showSdStorage.value) }
    var orientation by remember { mutableStateOf(AppOrientation.mode(context)) }
    var uiScale by remember { mutableStateOf(AppThemeState.uiScale.value) }
    var fontScale by remember { mutableStateOf(AppThemeState.fontScale.value) }
    var captureEnabled by remember { mutableStateOf(WinFgCapture.isEnabled(context)) }
    var captureRes by remember { mutableStateOf(WinFgCapture.captureRes(context)) }
    var captureLogging by remember { mutableStateOf(WinFgDiag.isExtraLoggingEnabled(context)) }

    // ── Sound (MIDI Sound Font) — same as SettingsScreen's Sound section. ──
    var sfNames by remember { mutableStateOf(listOf<String>()) }
    var selectedSF by remember { mutableStateOf(0) }
    fun refreshSF() {
        val names = mutableListOf(MidiManager.DEFAULT_SF2_FILE)
        val files = MidiManager.getSoundFontDir(context).listFiles()
        if (files != null) for (file in files) if (file.name.endsWith(".sf2")) names.add(file.name)
        sfNames = names
    }

    // ── ImageFS backup/restore — same as SettingsScreen's ImageFS section. ──
    val activity = context as? Activity
    val mainActivity = context as? MainActivity
    var showBackupDialog by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    fun beginRestoreFromUri(uri: Uri) {
        pendingRestoreUri = uri
        showRestoreConfirm = true
    }

    // ── Frame Generation — LSFG Native (Lossless Scaling DLL) — same as SettingsScreen. ──
    val lsfgDllFile = remember { File(context.filesDir, "lsfg-vk/Lossless.dll") }
    fun lsfgDllStatusText(): String {
        if (!(lsfgDllFile.isFile && lsfgDllFile.length() > 0)) return "Not set — LSFG Native will stay off"
        val mb = lsfgDllFile.length() / (1024 * 1024)
        return when (prefs.getString("lsfg_dll_source", null)) {
            "store"  -> "Imported from Steam store (Lossless Scaling) — $mb MB"
            "manual" -> "Imported manually — $mb MB"
            else     -> "Imported ($mb MB)"
        }
    }
    var lsfgDllStatus by remember { mutableStateOf(lsfgDllStatusText()) }
    var lsfgShaderStatus by remember { mutableStateOf("") }
    val lsfgShaderBuilding = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    fun buildLsfgShaderCache() {
        if (!(lsfgDllFile.isFile && lsfgDllFile.length() > 0)) { lsfgShaderStatus = ""; return }
        if (!lsfgShaderBuilding.compareAndSet(false, true)) return
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        val appCtx = context.applicationContext
        lsfgShaderStatus = "Preparing shaders for LSFG Native… (one-time, can take a minute)"
        Thread {
            val started = System.currentTimeMillis()
            val status = try { com.winlator.star.core.LsfgNative.ensureCache(appCtx, false) } catch (e: Throwable) { -1 }
            val took = (System.currentTimeMillis() - started) / 1000
            val text = when (status) {
                com.winlator.star.core.LsfgNative.STATUS_OK ->
                    if (took >= 2) "Shaders ready for LSFG Native (built in ${took}s)" else "Shaders ready for LSFG Native"
                -1 -> "Shader preparation failed; it will be retried when a game launches"
                else -> com.winlator.star.core.LsfgNative.explain(status)
            }
            main.post { lsfgShaderStatus = text; lsfgShaderBuilding.set(false) }
        }.start()
    }
    fun importLosslessDllFromUri(uri: Uri) {
        try {
            lsfgDllFile.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input ->
                lsfgDllFile.outputStream().use { output -> input.copyTo(output) }
            }
            prefs.edit().putString("lsfg_dll_source", "manual").apply()
            lsfgDllStatus = lsfgDllStatusText()
            buildLsfgShaderCache()
        } catch (e: Exception) {
            lsfgDllStatus = "Import failed: " + e.message
        }
    }
    fun findStoreLosslessDll(): File? {
        val root = File(context.filesDir, "imagefs/steam_games")
        if (!root.isDirectory) return null
        return root.walkTopDown().maxDepth(6)
            .filter { it.isFile && it.name.equals("Lossless.dll", ignoreCase = true) && it.length() > 0 }
            .maxByOrNull { it.lastModified() }
    }
    fun detectLosslessDllFromStore() {
        val src = findStoreLosslessDll()
        if (src == null) {
            Toast.makeText(context, "No Steam-store Lossless Scaling found — download it from the store or import manually.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            lsfgDllFile.parentFile?.mkdirs()
            src.inputStream().use { input -> lsfgDllFile.outputStream().use { output -> input.copyTo(output) } }
            prefs.edit().putString("lsfg_dll_source", "store").apply()
            lsfgDllStatus = lsfgDllStatusText()
            buildLsfgShaderCache()
            Toast.makeText(context, "Lossless.dll set from Steam store install.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            lsfgDllStatus = "Detect failed: " + e.message
            Toast.makeText(context, "Detect failed: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    // ── In-app pickers (primary actions, matching SettingsScreen). ──
    val installSFInAppLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = if (result.resultCode == Activity.RESULT_OK) InAppFilePicker.pickedUri(result.data) else null
        if (uri != null && activity != null) {
            val dialog = PreloaderDialog(activity)
            dialog.showOnUiThread(R.string.installing_content)
            MidiManager.installSF2File(context, uri, object : MidiManager.OnSoundFontInstalledCallback {
                override fun onSuccess() {
                    dialog.closeOnUiThread()
                    activity.runOnUiThread {
                        ContentDialog.alert(context, R.string.sound_font_installed_success, null)
                        refreshSF()
                    }
                }
                override fun onFailed(reason: Int) {
                    dialog.closeOnUiThread()
                    val resId = when (reason) {
                        MidiManager.ERROR_BADFORMAT -> R.string.sound_font_bad_format
                        MidiManager.ERROR_EXIST -> R.string.sound_font_already_exist
                        else -> R.string.sound_font_installed_failed
                    }
                    activity.runOnUiThread { ContentDialog.alert(context, resId, null) }
                }
            })
        }
    }
    val restoreFileInAppLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) InAppFilePicker.pickedUri(result.data)?.let { beginRestoreFromUri(it) }
    }
    val importLosslessDllInAppLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) InAppFilePicker.pickedUri(result.data)?.let { importLosslessDllFromUri(it) }
    }

    // ── Seed the SoundFont list + warm the LSFG shader cache on entry (as SettingsScreen does). ──
    LaunchedEffect(Unit) {
        refreshSF()
        buildLsfgShaderCache()
    }

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

        // ── Sound ──
        DeckGroup(title = "Sound") {
            DeckChoiceRow(
                label = "MIDI Sound Font",
                options = sfNames.map { it to it },
                selected = sfNames.getOrElse(selectedSF) { MidiManager.DEFAULT_SF2_FILE },
                onPick = { picked -> selectedSF = sfNames.indexOf(picked).coerceAtLeast(0) },
            )
            DeckRow(label = "Install / Remove", hint = "Add a .sf2 or remove a custom one") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeckButton("Install") {
                        installSFInAppLauncher.launch(
                            InAppFilePicker.buildIntent(context, InAppFilePicker.SF2, "Select SoundFont"),
                        )
                    }
                    DeckButton("Remove", primary = false) {
                        if (selectedSF != 0) {
                            ContentDialog.confirm(context, R.string.do_you_want_to_remove_this_sound_font) {
                                if (MidiManager.removeSF2File(context, sfNames[selectedSF])) {
                                    AppUtils.showToast(context, R.string.sound_font_removed_success)
                                    refreshSF()
                                } else AppUtils.showToast(context, R.string.sound_font_removed_failed)
                            }
                        } else AppUtils.showToast(context, R.string.cannot_remove_default_sound_font)
                    }
                }
            }
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
                    AppThemeState.setShowStores(it)
                }
            }
            DeckRow(label = "Show internal storage") {
                DeckToggle(showInternal) {
                    showInternal = it
                    AppThemeState.setShowInternalStorage(it)
                }
            }
            DeckRow(label = "Show SD card storage") {
                DeckToggle(showSd) {
                    showSd = it
                    AppThemeState.setShowSdStorage(it)
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
                    AppThemeState.setUiScale(it)
                },
            )
            DeckSliderRow(
                label = "Font Size",
                value = fontScale,
                range = 0.5f..1.5f,
                format = { "${(it * 100).toInt()}%" },
                onChange = {
                    fontScale = it
                    AppThemeState.setFontScale(it)
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

        // ── ImageFS ──
        DeckGroup(title = "ImageFS") {
            DeckRow(label = "Reinstall ImageFS", hint = "Restore system files; containers unchanged") {
                DeckButton("Reinstall", primary = false) {
                    ContentDialog.confirm(context, R.string.do_you_want_to_reinstall_imagefs) {
                        mainActivity?.let { ImageFsInstaller.installFromAssets(it) }
                    }
                }
            }
            DeckRow(label = "Backup Data", hint = "Archive the app data directory") {
                DeckButton("Backup", primary = false) { showBackupDialog = true }
            }
            DeckRow(label = "Restore Data", hint = "Restore from a backup (restarts the app)") {
                DeckButton("Restore", primary = false) {
                    restoreFileInAppLauncher.launch(
                        InAppFilePicker.buildIntent(context, InAppFilePicker.SAVE, "Select backup"),
                    )
                }
            }
        }

        // ── Frame Generation — LSFG Native (Lossless Scaling) ──
        DeckGroup(title = "Frame Generation — LSFG Native") {
            DeckRow(label = "Status", hint = lsfgDllStatus) {
                if (lsfgShaderStatus.isNotEmpty()) {
                    Text(lsfgShaderStatus, color = DeckPalette.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            DeckRow(label = "Detect from Steam store", hint = "Find Lossless.dll in a store install") {
                DeckButton("Detect", primary = false) { detectLosslessDllFromStore() }
            }
            DeckRow(label = "Import Lossless.dll", hint = "Pick your own copy") {
                DeckButton("Import", primary = false) {
                    importLosslessDllInAppLauncher.launch(
                        InAppFilePicker.buildIntent(context, InAppFilePicker.DLL, "Select Lossless.dll"),
                    )
                }
            }
            if (lsfgDllFile.isFile) {
                DeckRow(label = "Remove Lossless.dll") {
                    DeckButton("Remove", primary = false) {
                        lsfgDllFile.delete()
                        com.winlator.star.core.LsfgNative.cacheFile(context).delete()
                        prefs.edit().remove("lsfg_dll_source").apply()
                        lsfgDllStatus = lsfgDllStatusText()
                        lsfgShaderStatus = ""
                    }
                }
            }
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

    // ── ImageFS backup/restore dialogs (same as SettingsScreen). ──
    if (isBackingUp) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeckPalette.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Backing up data...")
            }
        }
    }
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup Data") },
            text = { Text("Do you want to create a backup of the app's data directory?") },
            confirmButton = {
                TextButton(onClick = {
                    showBackupDialog = false
                    isBackingUp = true
                    val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
                    executor.execute {
                        val dataDir = context.filesDir.parentFile
                        val backupFile = File(Environment.getExternalStorageDirectory(), "app_data_backup.tar")
                        try {
                            com.winlator.star.core.TarCompressorUtils.archive(
                                arrayOf(dataDir), backupFile,
                            ) { file -> !file.absolutePath.contains("imagefs/tmp/.sysvshm") }
                            (context as? Activity)?.runOnUiThread {
                                isBackingUp = false
                                AppUtils.showToast(context, "Backup completed: ${backupFile.path}")
                            }
                        } catch (_: Exception) {
                            (context as? Activity)?.runOnUiThread {
                                isBackingUp = false
                                AppUtils.showToast(context, "Backup failed.")
                            }
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = { TextButton(onClick = { showBackupDialog = false }) { Text("No") } },
        )
    }
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false; pendingRestoreUri = null },
            title = { Text("Restore Data") },
            text = { Text("This will restart the app. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    pendingRestoreUri?.let { uri ->
                        val intent = Intent(context, com.winlator.star.restore.RestoreActivity::class.java)
                        intent.data = uri
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { showRestoreConfirm = false; pendingRestoreUri = null }) { Text("Cancel") } },
        )
    }
}
