package com.winlator.star.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.winlator.star.core.FileUtils
import com.winlator.star.store.SteamStoreSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.min

/** One game on the hub carousel: display art plus the launchable shortcut (null = featured art only). */
internal data class HubEntry(
    val title: String,
    val bitmap: Bitmap?,
    val game: HubGame?,
)

/** A user-added game: where its .desktop lives, which container runs it, and its hub artwork. */
internal data class HubGame(
    val name: String,
    val containerId: Int,
    val shortcutPath: String,
    val appId: Int?,
    val coverPath: String?,
)

/** filesDir/hub_games.json — the user's Add Games picks, persisted across launches. */
internal object HubGamesStore {
    private const val FILE_NAME = "hub_games.json"

    fun load(context: Context): List<HubGame> {
        return runCatching {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.isFile) return emptyList()
            val raw = FileUtils.readString(file) ?: return emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name")
            if (name.isEmpty()) return@mapNotNull null
            HubGame(
                name = name,
                containerId = o.optInt("containerId", -1),
                shortcutPath = o.optString("shortcutPath"),
                appId = if (o.isNull("appId")) null else o.optInt("appId"),
                coverPath = o.optString("coverPath").takeIf { it.isNotEmpty() },
            )
        }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, game: HubGame) {
        runCatching {
            val arr = JSONArray()
            (load(context) + game).forEach { g ->
                arr.put(JSONObject().apply {
                    put("name", g.name)
                    put("containerId", g.containerId)
                    put("shortcutPath", g.shortcutPath)
                    put("appId", g.appId ?: JSONObject.NULL)
                    put("coverPath", g.coverPath ?: JSONObject.NULL)
                })
            }
            FileUtils.writeString(File(context.filesDir, FILE_NAME), arr.toString())
        }
    }
}

/** SAF uri → a real cache file (identifier/importer need a path). Blocking — call off the main thread. */
internal fun copyPickedGame(context: Context, uri: Uri): File? {
    return runCatching {
        val name = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "game.exe"
    val dir = File(context.cacheDir, "hub_pick").apply { mkdirs() }
    val out = File(dir, name.replace('/', '_').ifEmpty { "game.exe" })
    val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream -> out.outputStream().use { stream.copyTo(it) } }
        out
    }.getOrNull()
}

/** Official Steam 600x900 library art for the hub tile, cached under filesDir/hub_covers. Blocking. */
internal fun fetchHubCover(context: Context, appId: Int): String? {
    return runCatching {
        val dir = File(context.filesDir, "hub_covers").apply { mkdirs() }
    val out = File(dir, "$appId.jpg")
    if (out.isFile && out.length() > 5000) return out.absolutePath
    val conn = URL(SteamStoreSearch.coverUrl(appId)).openConnection() as HttpURLConnection
    conn.connectTimeout = 15000
    conn.readTimeout = 30000
    conn.inputStream.use { ins -> out.outputStream().use { ins.copyTo(it) } }
        if (out.length() < 5000) { out.delete(); return null }
        out.absolutePath
    }.getOrNull()
}

/**
 * The Home tab's primary action: a dark-blue → signal-blue gradient pill, the DroidDeck
 * controller-friendly CTA. Used for "Start Hubbing" and "Add Games".
 */
@Composable
internal fun HubGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0A2E52), Color(0xFF1A9FFF)),
                ),
            )
            .border(1.dp, Color(0xFF59C1FF).copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/**
 * The games library: a 3D coverflow carousel. The selected game sits center, big, outlined in the
 * deck blue; neighbours fan out left and right, rotated toward the center and scaled down with
 * distance — tap or swipe (or D-pad focus + select) to move the selection.
 */
@Composable
internal fun HubCarousel(
    entries: List<HubEntry>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    val current = selected.coerceIn(0, entries.size - 1)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(entries.size, current) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragEnd = { total = 0f },
                    onDragCancel = { total = 0f },
                ) { _, dx ->
                    total += dx
                    if (total < -60f) {
                        total = 0f
                        onSelect((current + 1).coerceAtMost(entries.size - 1))
                    } else if (total > 60f) {
                        total = 0f
                        onSelect((current - 1).coerceAtLeast(0))
                    }
                }
            },
    ) {
        entries.forEachIndexed { index, entry ->
            val delta = index - current
            val distance = abs(delta)
            if (distance > 4) return@forEachIndexed
            val isSelected = delta == 0

            val scale by animateFloatAsState(
                if (isSelected) 1.16f else 0.74f - 0.05f * min(distance, 3),
                label = "hubScale",
            )
            val rotY by animateFloatAsState(
                when {
                    isSelected -> 0f
                    delta < 0 -> 42f - 6f * min(distance, 3)
                    else -> -42f + 6f * min(distance, 3)
                },
                label = "hubRot",
            )
            val translateX by animateFloatAsState(delta * 122f, label = "hubX")
            val alphaAnim by animateFloatAsState(
                if (isSelected) 1f else 0.82f - 0.15f * min(distance, 3),
                label = "hubAlpha",
            )

            Box(
                modifier = Modifier
                    .size(width = 170.dp, height = 255.dp)
                    .zIndex((10 - distance).toFloat())
                    .offset(x = translateX.dp)
                    .graphicsLayer {
                        rotationY = rotY
                        scaleX = scale
                        scaleY = scale
                        alpha = alphaAnim
                        cameraDistance = 12f * density
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(DeckPalette.surface)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) DeckPalette.primary else DeckPalette.line,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(index) },
            ) {
                val bitmap = entry.bitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    val hue = remember(entry.title) { hueOf(entry.title) }
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.hsv(hue, 0.55f, 0.75f),
                                        Color.hsv((hue + 0.12f) % 1f, 0.60f, 0.45f),
                                    ),
                                ),
                            ),
                    )
                }
                if (isSelected) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    0.72f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.45f),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * "Is the game correct?" — the modernized confirm popup for a picked .exe: editable name field,
 * a container pick when there's more than one, and "Search on Steam" to scrape the store
 * metadata (name + official 600x900 art) before the game joins the carousel.
 */
@Composable
internal fun HubConfirmDialog(
    file: File,
    containerNames: List<String>,
    initialName: String,
    initialAppId: Int?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, appId: Int?, containerIndex: Int, coverPath: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }
    var appId by remember { mutableStateOf(initialAppId) }
    var coverPath by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<SteamStoreSearch.SteamSuggestion>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var containerIdx by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(DeckPalette.groupShape)
                .background(DeckPalette.surface)
                .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Is the game correct?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeckPalette.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = file.name,
                    fontSize = 12.5.sp,
                    color = DeckPalette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Game name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (containerNames.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(DeckPalette.surfaceVariant)
                                .clickable { menuOpen = true }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                        ) {
                            Text(
                                text = "Container · ${containerNames[containerIdx.coerceIn(0, containerNames.size - 1)]}",
                                fontSize = 13.sp,
                                color = DeckPalette.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            modifier = Modifier.background(DeckPalette.surfaceVariant),
                        ) {
                            containerNames.forEachIndexed { i, label ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(label, color = DeckPalette.onBackground, fontSize = 14.sp) },
                                    onClick = { containerIdx = i; menuOpen = false },
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No container yet — create one in Containers first.",
                        fontSize = 12.5.sp,
                        color = DeckPalette.error,
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HubGradientButton("Search on Steam") {
                        if (name.isBlank()) return@HubGradientButton
                        searching = true
                        results = null
                        scope.launch(Dispatchers.IO) {
                            val hits = SteamStoreSearch.searchByName(name)
                            withContext(Dispatchers.Main) {
                                results = hits
                                searching = false
                            }
                        }
                    }
                    if (searching) {
                        Spacer(Modifier.width(12.dp))
                        CircularProgressIndicator(
                            color = DeckPalette.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                results?.let { hits ->
                    Spacer(Modifier.height(10.dp))
                    if (hits.isEmpty()) {
                        Text(
                            text = "No Steam matches — add it with the typed name.",
                            fontSize = 12.5.sp,
                            color = DeckPalette.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 190.dp)) {
                            items(hits) { hit ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            name = hit.name
                                            appId = hit.appId
                                            results = null
                                            scope.launch(Dispatchers.IO) {
                                                val path = fetchHubCover(context, hit.appId)
                                                withContext(Dispatchers.Main) { coverPath = path }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 11.dp),
                                ) {
                                    Text(
                                        text = hit.name,
                                        fontSize = 14.sp,
                                        color = DeckPalette.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "Steam",
                                        fontSize = 11.sp,
                                        color = DeckPalette.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                if (coverPath != null && appId != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val cover = remember(coverPath) { BitmapFactory.decodeFile(coverPath) }
                        if (cover != null) {
                            Image(
                                bitmap = cover.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 46.dp, height = 69.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = "Steam match found ✓",
                            fontSize = 13.sp,
                            color = DeckPalette.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel", color = DeckPalette.onBackground)
                    }
                    Spacer(Modifier.width(10.dp))
                    HubGradientButton("Add") {
                        onConfirm(name.trim().ifEmpty { file.nameWithoutExtension }, appId, containerIdx, coverPath)
                    }
                }
            }
        }
    }
}
