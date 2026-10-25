package com.winhub.hernandez.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.winhub.hernandez.store.EpicApiClient
import com.winhub.hernandez.store.EpicCancelPolicy
import com.winhub.hernandez.store.EpicCredentialStore
import com.winhub.hernandez.store.EpicDownloadManager
import com.winhub.hernandez.store.EpicGame
import com.winhub.hernandez.store.EpicInstallState
import com.winhub.hernandez.store.EpicInstallTags
import com.winhub.hernandez.store.deckResolveExe
import com.winhub.hernandez.store.download.DownloadRegistry
import com.winhub.hernandez.store.download.DownloadScope
import com.winhub.hernandez.store.download.Store
import com.winhub.hernandez.store.download.StoreDownloadHooks
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-Deck Epic install. Drives the REAL [EpicDownloadManager] engine — same token → manifest →
 * install sequence the legacy list screen runs — but renders entirely in the Deck UI so install
 * never leaves the Deck shell. Progress survives screen disposal: the work runs on
 * [DownloadScope] with a foreground Download Manager row, exactly like the legacy detail page.
 */
@Composable
internal fun DeckEpicInstall(game: EpicGame, onBack: () -> Unit, onOpenStore: (() -> Unit)? = null) {
    val context = LocalContext.current
    val appCtx = remember(context) { context.applicationContext }
    val prefs = remember(appCtx) { appCtx.getSharedPreferences("bh_epic_prefs", 0) }
    val main = remember { Handler(Looper.getMainLooper()) }
    var pct by remember { mutableStateOf(-1) }
    var status by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var started by remember { mutableStateOf(false) }
    val cancelled = remember { AtomicBoolean(false) }
    val installed = EpicInstallState.isInstalled(context, game.appName)

    DeckInstallShell(
        title = game.title,
        art = game.artCover.ifBlank { null } ?: game.artSquare.ifBlank { null },
        dev = game.developer,
        onBack = onBack,
        progress = pct,
        status = status,
        finished = finished,
        error = errorText,
        started = started,
        storeLabel = "Epic",
        onOpenStore = onOpenStore,
        description = game.description ?: "",
        releaseDate = game.releaseDate ?: "",
        installed = installed,
        addGame = DeckAddRequest(
            title = game.title,
            store = "Epic",
            coverUrl = game.artCover.ifBlank { null } ?: game.artSquare.ifBlank { null },
            resolveExe = { prefs.getString("epic_exe_${game.appName}", null) },
        ),
        onStart = {
            errorText = null
            started = true
            pct = 0
            status = "Fetching manifest..."
            cancelled.set(false)
            EpicCancelPolicy.clear(game.appName)
            DownloadRegistry.init(appCtx)
            StoreDownloadHooks.registerDownload(
                store = Store.EPIC,
                id = game.appName,
                name = game.title,
                cover = game.artCover.ifEmpty { game.artSquare },
                supportsPause = false,
                installTotal = prefs.getLong("epic_size_${game.appName}", 0L),
                cancel = { cancelled.set(true) },
            )
            DownloadScope.io.launch {
                try {
                    val token = EpicCredentialStore.getValidAccessToken(appCtx)
                    if (token == null) {
                        StoreDownloadHooks.markFailed(Store.EPIC, game.appName, "Login required")
                        main.post { errorText = "Login required"; started = false }
                        return@launch
                    }
                    val manifestJson = EpicApiClient.getManifestApiJson(
                        token, game.namespace, game.catalogItemId, game.appName,
                    )
                    if (manifestJson == null) {
                        StoreDownloadHooks.markFailed(Store.EPIC, game.appName, "Failed to fetch manifest")
                        main.post { errorText = "Failed to fetch manifest"; started = false }
                        return@launch
                    }
                    var sanitized = game.title.replace(Regex("[^a-zA-Z0-9 \\-_]"), "").trim()
                    if (sanitized.isEmpty()) sanitized = "epic_${game.appName.hashCode()}"
                    val installDir = File(File(appCtx.filesDir, "imagefs/epic_games"), sanitized)
                    prefs.edit().putString("epic_dir_${game.appName}", installDir.absolutePath).apply()
                    val installTags = EpicInstallTags.tagsForCurrentContainer(appCtx)

                    val ok = EpicDownloadManager.install(
                        appCtx, manifestJson, token, installDir.absolutePath, installTags, cancelled,
                    ) { msg, p ->
                        if (!cancelled.get()) {
                            StoreDownloadHooks.tick(Store.EPIC, game.appName, p)
                            main.post { pct = p; status = msg }
                        }
                    }

                    if (cancelled.get()) {
                        StoreDownloadHooks.markCancelled(Store.EPIC, game.appName)
                        main.post { started = false; pct = -1; status = "" }
                        return@launch
                    }
                    if (!ok) {
                        StoreDownloadHooks.markFailed(Store.EPIC, game.appName, "Download failed")
                        main.post { errorText = "Download failed"; started = false }
                        return@launch
                    }
                    val exePath = deckResolveExe(installDir, game.title)
                    if (exePath == null) {
                        StoreDownloadHooks.markFailed(Store.EPIC, game.appName, "No executable found")
                        main.post { errorText = "No executable found"; started = false }
                        return@launch
                    }
                    prefs.edit().putString("epic_exe_${game.appName}", exePath).apply()
                    StoreDownloadHooks.markInstalled(
                        store = Store.EPIC,
                        id = game.appName,
                        installPath = installDir.absolutePath,
                        bytes = prefs.getLong("epic_size_${game.appName}", 0L),
                    )
                    main.post { finished = true; pct = 100; status = "Installed." }
                } catch (e: Exception) {
                    if (!cancelled.get()) {
                        StoreDownloadHooks.markFailed(Store.EPIC, game.appName, e.message ?: "Unknown error")
                        main.post { errorText = e.message ?: "Unknown error"; started = false }
                    }
                }
            }
        },
        onCancel = {
            cancelled.set(true)
            status = "Cancelling..."
        },
    )
}
