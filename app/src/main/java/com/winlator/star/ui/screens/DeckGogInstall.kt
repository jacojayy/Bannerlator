package com.winlator.star.ui.screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.winlator.star.store.GogDownloadManager
import com.winlator.star.store.GogGame
import com.winlator.star.store.GogLibraryRepo
import com.winlator.star.store.StoreNet
import com.winlator.star.store.download.Store
import com.winlator.star.store.download.StoreDownloadHooks
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
 * Activity uses — and publishes progress into the shared Download Manager, but renders entirely in
 * the Deck UI so install never leaves the Deck shell.
 */
@Composable
internal fun DeckGogInstall(game: GogGame, onBack: () -> Unit) {
    val context = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }
    var pct by remember { mutableStateOf(-1) }
    var status by remember { mutableStateOf("") }
    var finished by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var cancelHandle by remember { mutableStateOf<Runnable?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Deck.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text("Back", color = Deck.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))

        val shape = RoundedCornerShape(12.dp)
        if (game.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = game.imageUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(shape)
                    .background(Deck.surface)
                    .border(1.dp, Deck.line, shape),
            )
            Spacer(Modifier.size(12.dp))
        }
        Text(
            text = game.title,
            color = Deck.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
        )
        if (game.developer.isNotEmpty()) {
            Text(game.developer, color = Deck.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.size(18.dp))

        val err = errorText
        when {
            err != null -> Text(err, color = Color(0xFFFF6B6B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            finished -> Text("Installed.", color = Deck.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            pct < 0 -> DeckButton("Install", primary = true) {
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
            }
            else -> {
                LinearProgressIndicator(
                    progress = { (pct.coerceIn(0, 100)) / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Deck.primary,
                    trackColor = Deck.surfaceVariant,
                )
                Spacer(Modifier.size(8.dp))
                Text(status, color = Deck.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.size(12.dp))
                DeckButton("Cancel", primary = false) { cancelHandle?.run() }
            }
        }
    }
}
