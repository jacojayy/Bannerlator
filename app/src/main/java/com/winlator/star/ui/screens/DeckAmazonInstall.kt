package com.winlator.star.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.winlator.star.store.AmazonCredentialStore
import com.winlator.star.store.AmazonDownloadManager
import com.winlator.star.store.AmazonGame
import com.winlator.star.store.deckResolveExe
import com.winlator.star.store.download.DownloadRegistry
import com.winlator.star.store.download.DownloadScope
import com.winlator.star.store.download.Store
import com.winlator.star.store.download.StoreDownloadHooks
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * In-Deck Amazon install. Drives the REAL [AmazonDownloadManager] engine — same token → dir →
 * install sequence the legacy list screen runs — but renders entirely in the Deck UI so install
 * never leaves the Deck shell. Uses the Wine-mapped `imagefs/Amazon` root (the list screen's
 * path), so the finished game lands at `Z:\Amazon\...` like every legacy install.
 */
@Composable
internal fun DeckAmazonInstall(game: AmazonGame, onBack: () -> Unit, onOpenStore: (() -> Unit)? = null) {
    val context = LocalContext.current
    val appCtx = remember(context) { context.applicationContext }
    val prefs = remember(appCtx) { appCtx.getSharedPreferences("bh_amazon_prefs", 0) }
    val main = remember { Handler(Looper.getMainLooper()) }
    var pct by remember { mutableStateOf(-1) }
    var status by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var started by remember { mutableStateOf(false) }
    val cancelled = remember { AtomicBoolean(false) }
    val installed = prefs.getString("amazon_exe_${game.productId}", null) != null

    DeckInstallShell(
        title = game.title,
        art = game.artUrl.ifBlank { null } ?: game.heroUrl.ifBlank { null },
        dev = game.developer,
        onBack = onBack,
        progress = pct,
        status = status,
        finished = finished,
        error = errorText,
        started = started,
        storeLabel = "Amazon",
        onOpenStore = onOpenStore,
        publisher = game.publisher ?: "",
        installed = installed,
        addGame = DeckAddRequest(
            title = game.title,
            store = "Amazon",
            coverUrl = game.artUrl.ifBlank { null } ?: game.heroUrl.ifBlank { null },
            resolveExe = { prefs.getString("amazon_exe_${game.productId}", null) },
        ),
        onStart = {
            errorText = null
            started = true
            pct = 0
            status = "Connecting..."
            cancelled.set(false)
            DownloadRegistry.init(appCtx)
            DownloadScope.io.launch {
                try {
                    val token = AmazonCredentialStore.getValidAccessToken(appCtx)
                    if (token == null) {
                        StoreDownloadHooks.markFailed(Store.AMAZON, game.productId, "Login required")
                        main.post { errorText = "Login required"; started = false }
                        return@launch
                    }
                    var sanitized = game.title.replace("[^a-zA-Z0-9 \\-_]".toRegex(), "").trim()
                    if (sanitized.isEmpty()) sanitized = "game_${game.productId.hashCode()}"
                    val installDir = File(File(appCtx.filesDir, "imagefs/Amazon"), sanitized)
                    prefs.edit().putString("amazon_dir_${game.productId}", installDir.absolutePath).apply()

                    StoreDownloadHooks.registerDownload(
                        store = Store.AMAZON,
                        id = game.productId,
                        name = game.title,
                        cover = game.artUrl.ifEmpty { null },
                        supportsPause = false,
                        installTotal = prefs.getLong("amazon_size_${game.productId}", 0L),
                        cancel = { cancelled.set(true) },
                    )

                    val ok = AmazonDownloadManager.install(
                        appCtx, game, token, installDir,
                        { dl, total, file ->
                            if (!cancelled.get()) {
                                val p = if (total > 0) (dl * 100L / total).toInt() else 0
                                StoreDownloadHooks.tick(
                                    store = Store.AMAZON,
                                    id = game.productId,
                                    pct = p,
                                    installDone = dl,
                                    installTotal = total,
                                )
                                main.post {
                                    pct = p
                                    status = if (!file.isNullOrEmpty()) file else "Downloading..."
                                }
                            }
                        },
                        { cancelled.get() },
                    )

                    if (cancelled.get()) {
                        StoreDownloadHooks.markCancelled(Store.AMAZON, game.productId)
                        main.post { started = false; pct = -1; status = "" }
                        return@launch
                    }
                    if (!ok) {
                        StoreDownloadHooks.markFailed(Store.AMAZON, game.productId, "Download failed")
                        main.post { errorText = "Download failed"; started = false }
                        return@launch
                    }
                    val exePath = deckResolveExe(installDir, game.title)
                    if (exePath == null) {
                        StoreDownloadHooks.markFailed(Store.AMAZON, game.productId, "No executable found")
                        main.post { errorText = "No executable found"; started = false }
                        return@launch
                    }
                    prefs.edit().putString("amazon_exe_${game.productId}", exePath).apply()
                    StoreDownloadHooks.markInstalled(
                        store = Store.AMAZON,
                        id = game.productId,
                        installPath = installDir.absolutePath,
                        bytes = prefs.getLong("amazon_size_${game.productId}", 0L),
                    )
                    main.post { finished = true; pct = 100; status = "Installed." }
                } catch (e: Exception) {
                    if (!cancelled.get()) {
                        StoreDownloadHooks.markFailed(Store.AMAZON, game.productId, e.message ?: "Unknown error")
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
