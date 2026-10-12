package com.winlator.star.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.winlator.star.store.SteamDepotDownloader
import com.winlator.star.store.SteamForegroundService
import com.winlator.star.store.SteamPrefs
import com.winlator.star.store.SteamRepository
import com.winlator.star.store.download.DownloadRegistry
import com.winlator.star.store.download.DownloadScope
import com.winlator.star.store.download.DownloadState
import com.winlator.star.store.download.Store
import kotlinx.coroutines.launch

/** What a Deck Steam card hands to [DeckSteamInstall]: the app plus display strings. */
internal data class DeckSteamTarget(val appId: Int, val title: String, val art: String?)

/**
 * In-Deck Steam install. Starts the REAL [SteamDepotDownloader] (depot/manifest/credential work
 * all stay internal — the call needs only the appId) and reads live progress straight from the
 * shared [DownloadRegistry] StateFlow the downloader already writes. Steam does not use
 * StoreDownloadHooks — the downloader wires the registry row itself — so this screen only starts
 * the download and observes the row. Defaults: Fast tier, internal storage (no speed/SD dialog).
 */
@Composable
internal fun DeckSteamInstall(target: DeckSteamTarget, onBack: () -> Unit) {
    val context = LocalContext.current
    val appCtx = remember(context) { context.applicationContext }
    val main = remember { Handler(Looper.getMainLooper()) }
    val entries by DownloadRegistry.entries.collectAsState()
    val row = entries.firstOrNull { it.key == "${Store.STEAM}:${target.appId}" }
    var starting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var handle by remember { mutableStateOf<SteamDepotDownloader.DownloadControl?>(null) }

    val active = row != null && (
        row.state == DownloadState.DOWNLOADING ||
            row.state == DownloadState.QUEUED ||
            row.state == DownloadState.PAUSED
        )
    val started = starting || active
    val finished = row?.state == DownloadState.INSTALLED
    val rowError =
        if (row?.state == DownloadState.FAILED) row.error ?: "Download failed" else null
    val error = if (started) null else (errorText ?: rowError)
    val status = when {
        row == null -> "Starting..."
        row.state == DownloadState.QUEUED -> "Queued..."
        row.state == DownloadState.PAUSED -> "Paused"
        else -> "${row.pct}%"
    }

    // A live registry row supersedes the local "starting" flag (it also clears it after a
    // pre-row precondition error is retried once the row appears).
    LaunchedEffect(row != null) {
        if (row != null) starting = false
    }

    DeckInstallShell(
        title = target.title,
        art = target.art,
        dev = "",
        onBack = onBack,
        progress = row?.pct ?: 0,
        status = status,
        finished = finished,
        error = error,
        started = started,
        onStart = {
            errorText = null
            starting = true
            DownloadScope.io.launch {
                try {
                    DownloadRegistry.init(appCtx)
                    if (SteamPrefs.isLoggedIn) SteamForegroundService.start(appCtx)
                    val repo = SteamRepository.getInstance()
                    val err = when {
                        !repo.isLoggedIn -> "Steam not signed in"
                        repo.database.getGame(target.appId) == null -> "Game not in your library yet"
                        else -> null
                    }
                    if (err != null) {
                        main.post { starting = false; errorText = err }
                        return@launch
                    }
                    val control = SteamDepotDownloader.installApp(target.appId, appCtx)
                    main.post { handle = control }
                } catch (t: Throwable) {
                    main.post { starting = false; errorText = t.message ?: "Failed to start download" }
                }
            }
        },
        onCancel = {
            val control = handle
            if (control != null) {
                control.cancel.run()
                handle = null
            }
            starting = false
        },
    )
}
