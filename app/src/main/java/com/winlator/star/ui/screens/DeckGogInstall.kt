package com.winlator.star.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.winlator.star.store.GogDownloadManager
import com.winlator.star.store.GogGame
import com.winlator.star.store.GogInstallState
import com.winlator.star.store.GogLibraryRepo
import com.winlator.star.store.StoreNet
import com.winlator.star.store.download.Store
import com.winlator.star.store.download.StoreDownloadHooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Builds a real [GogGame] from a browse item, resolving the gen1/gen2 flag the download engine
 * needs. The flag is not carried by the browse catalog, so it is fetched from GOG's builds
 * endpoint exactly as the legacy repo does, then cached by the engine itself.
 */
internal suspend fun deckGogBuild(
    context: android.content.Context,
    id: String,
    title: String,
    imageUrl: String?,
    description: String,
    developer: String,
    category: String,
): GogGame {
    val token = GogLibraryRepo.validToken(context)
    val generation = runCatching {
        val json = StoreNet.get(
            "https://content-system.gog.com/products/$id/os/windows/builds?generation=2",
            bearer = token,
        ) ?: return@runCatching 1
        val items = JSONObject(json).optJSONArray("items") ?: return@runCatching 1
        var maxGen = 0
        for (i in 0 until items.length()) {
            val g = items.optJSONObject(i)?.optInt("generation", 0) ?: 0
            if (g > maxGen) maxGen = g
        }
        if (maxGen > 0) maxGen else 1
    }.getOrDefault(1)
    return GogGame(id, title, imageUrl ?: "", description, developer, category, generation)
}

/**
 * In-Deck GOG install. Drives the REAL [GogDownloadManager] engine — the same one the legacy detail
 * Activity uses — and publishes progress into the shared Download Manager, rendered through the
 * shared [DeckInstallShell] so install never leaves the Deck shell. Opens with an ownership check:
 * a browse title that is not in the library yet offers "Open in GOG" instead of a doomed install.
 */
@Composable
internal fun DeckGogInstall(game: GogGame, onBack: () -> Unit, onOpenStore: (() -> Unit)? = null) {
    val context = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    var pct by remember { mutableStateOf(-1) }
    var status by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var notOwned by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(true) }
    var cancelHandle by remember { mutableStateOf<Runnable?>(null) }

    // Browse items can be titles the account does not own — GOG's engine would just fail, so say
    // so up front and hand the user the door into GOG's own page for it. A failed check never
    // blocks an install (default true): the engine still enforces entitlements itself.
    LaunchedEffect(game.gameId) {
        val owned = withContext(Dispatchers.IO) {
            runCatching { GogLibraryRepo.cached(context).any { it.gameId == game.gameId } }
                .getOrDefault(true)
        }
        checking = false
        if (!owned) notOwned = true
    }

    val installed = GogInstallState.isInstalled(context, game.gameId)
    val prefs = context.getSharedPreferences("bh_gog_prefs", android.content.Context.MODE_PRIVATE)

    val shownError = when {
        errorText != null -> errorText
        notOwned -> "Not in your library yet."
        else -> null
    }

    DeckInstallShell(
        title = game.title,
        art = game.imageUrl.ifEmpty { null },
        dev = game.developer,
        onBack = onBack,
        progress = pct,
        status = status,
        finished = finished,
        error = shownError,
        started = pct >= 0,
        canInstall = !checking && !notOwned && errorText == null,
        onPauseResume = null,
        onOpenStore = if (!checking && (notOwned || errorText != null)) onOpenStore else null,
        storeLabel = "GOG",
        description = game.description ?: "",
        installed = installed,
        addGame = DeckAddRequest(
            title = game.title,
            coverUrl = game.imageUrl.ifEmpty { null },
            resolveExe = { prefs.getString("gog_exe_${game.gameId}", null) },
        ),
        onStart = {
            val cb = object : GogDownloadManager.Callback {
                override fun onProgress(msg: String, p: Int) {
                    StoreDownloadHooks.tick(Store.GOG, game.gameId, p)
                    main.post { pct = p; status = msg }
                }
                override fun onComplete(exePath: String) {
                    main.post { finished = true; pct = 100 }
                }
                override fun onError(msg: String) {
                    main.post { errorText = msg }
                }
                override fun onCancelled() {
                    main.post { status = "Cancelled"; pct = -1 }
                }
            }
            StoreDownloadHooks.registerDownload(
                store = Store.GOG,
                id = game.gameId,
                name = game.title,
                cover = game.imageUrl.ifEmpty { null },
                supportsPause = false,
                installTotal = 0L,
                cancel = { cancelHandle?.run() },
            )
            cancelHandle = GogDownloadManager.startDownload(context.applicationContext, game, cb)
        },
        onCancel = { cancelHandle?.run() },
    )
}
