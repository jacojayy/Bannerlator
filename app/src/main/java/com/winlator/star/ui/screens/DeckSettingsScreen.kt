package com.winlator.star.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.winlator.star.contents.ContentsManager

private val Box64Presets = listOf(
    "STABILITY", "COMPATIBILITY", "INTERMEDIATE", "PERFORMANCE", "PERFORMANCE_MALI",
    "EXTREME", "EXTREME_2", "UNITY", "UNITY_MONO_BLEEDING_EDGE", "DENUVO", "CUSTOM",
)

private val FexCorePresets = listOf(
    "STABILITY", "COMPATIBILITY", "INTERMEDIATE", "PERFORMANCE", "PERFORMANCE_TSO",
    "EXTREME", "EXTREME_TSO", "EXTREME_GN", "DENUVO", "CUSTOM",
)

/**
 * Deck Mode's settings page: the app's OWN preferences (same keys, same defaults, same Save
 * semantics as SettingsScreen) rendered in DroidDeck's group/row/chip language.
 */
@Composable
internal fun DeckSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

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
    var saved by remember { mutableStateOf(false) }

    fun save() {
        // Same writes as SettingsScreen.saveSettings — only the keys this page owns.
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
        editor.apply()
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
            text = "The same preferences as the Settings tab",
            fontSize = 13.sp,
            color = DeckPalette.onSurfaceVariant,
        )

        DeckGroup(title = "Appearance") {
            DeckRow(label = "Dark mode", hint = "Graphite surfaces everywhere") {
                DeckToggle(darkMode) { darkMode = it; saved = false }
            }
            DeckRow(label = "Enable Big Picture Mode on App Launch") {
                DeckToggle(bigPicture) { bigPicture = it; saved = false }
            }
            DeckChoiceRow(
                label = "Default landing screen",
                options = listOf(
                    "games" to "Games",
                    "containers" to "Containers",
                ),
                selected = landing,
                onPick = { landing = it; saved = false },
            )
        }

        DeckGroup(title = "Emulation") {
            DeckChoiceRow(
                label = "Box64 Preset",
                options = Box64Presets.map { it to DeckPalette.titleCase(it) },
                selected = box64,
                onPick = { box64 = it; saved = false },
            )
            DeckChoiceRow(
                label = "FEXCore Preset",
                options = FexCorePresets.map { it to DeckPalette.titleCase(it) },
                selected = fex,
                onPick = { fex = it; saved = false },
            )
        }

        DeckGroup(title = "Input") {
            DeckSliderRow(
                label = "Cursor Speed",
                hint = "Pointer speed inside the container",
                value = cursorSpeed,
                range = 0.1f..2.0f,
                format = { "${(it * 100).toInt()}%" },
                onChange = { cursorSpeed = it; saved = false },
            )
            DeckRow(label = "True Mouse Control", hint = "Deactivate with Volume Down") {
                DeckToggle(cursorLock) { cursorLock = it; saved = false }
            }
            DeckRow(label = "Disable Xinput", hint = "Used for exclusive M/KB support") {
                DeckToggle(xinputOff) { xinputOff = it; saved = false }
            }
            DeckRow(label = "Use DRI3 Extension") {
                DeckToggle(dri3) { dri3 = it; saved = false }
            }
            DeckRow(label = "Use XR") {
                DeckToggle(xr) { xr = it; saved = false }
            }
        }

        DeckGroup(title = "Integration") {
            DeckRow(label = "Enable File Provider") {
                DeckToggle(fileProvider) { fileProvider = it; saved = false }
            }
            DeckRow(label = "Open with Android Browser") {
                DeckToggle(androidBrowser) { androidBrowser = it; saved = false }
            }
            DeckRow(label = "Share Android Clipboard") {
                DeckToggle(shareClipboard) { shareClipboard = it; saved = false }
            }
        }

        DeckGroup(title = "SteamGrid API") {
            DeckRow(label = "Set SteamGrid API Key", hint = "Cover art for the library") {
                DeckToggle(apiKeyEnabled) { apiKeyEnabled = it; saved = false }
            }
            if (apiKeyEnabled) {
                DeckRow(label = "API Key") {
                    TextField(
                        value = apiKey,
                        onValueChange = { apiKey = it; saved = false },
                        singleLine = true,
                        visualTransformation = if (apiKey.isEmpty()) VisualTransformation.None else PasswordVisualTransformation(),
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
                            .width(220.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
        }

        DeckGroup(title = "Contents") {
            DeckRow(label = "Downloadable Contents URL") {
                TextField(
                    value = contentsUrl,
                    onValueChange = { contentsUrl = it; saved = false },
                    singleLine = true,
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
                        .width(300.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
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
}
