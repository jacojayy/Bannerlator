package com.winlator.star.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import android.provider.OpenableColumns
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.winlator.star.container.Shortcut
import com.winlator.star.core.FileUtils
import com.winlator.star.store.GoldbergComponent
import com.winlator.star.store.GoldbergMode
import com.winlator.star.store.GoldbergPatcher
import com.winlator.star.store.SteamDatabase
import com.winlator.star.store.SteamLiteComponent
import com.winlator.star.store.SteamLoginActivity
import com.winlator.star.store.SteamPrefs
import com.winlator.star.store.SteamSessionManager
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
    /** Store this came from ("Steam", "Epic", "GOG", "Amazon"); null when added by hand. */
    val store: String? = null,
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
                store = o.optString("store").takeIf { it.isNotEmpty() },
            )
        }
        }.getOrDefault(emptyList())
    }

    fun remove(context: Context, game: HubGame) {
        runCatching {
            val file = File(context.filesDir, FILE_NAME)
            val raw = FileUtils.readString(file) ?: return
            val arr = JSONArray(raw)
            val out = JSONArray()
            (0 until arr.length()).forEach { i ->
                val o = arr.optJSONObject(i) ?: return@forEach
                if (o.optString("shortcutPath") != game.shortcutPath) out.put(o)
            }
            FileUtils.writeString(file, out.toString())
        }
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
                    put("store", g.store ?: JSONObject.NULL)
                })
            }
            FileUtils.writeString(File(context.filesDir, FILE_NAME), arr.toString())
        }
    }
}

/** SAF uri → a real cache file (identifier/importer need a path). Blocking — call off the main thread. */
/**
 * Resolve a picked SAF uri to the on-disk executable IN PLACE — the same rule the old UI's
 * "+" importer uses, so the exe keeps its whole folder (dlls / data) beside it. Exotic providers
 * that can't be resolved fall back to a copy under Downloads/WinHub.
 */
internal fun resolvePickedGame(context: Context, uri: Uri): File? {
    val real = runCatching {
        if (uri.scheme == "file") return uri.path?.let { File(it) }
        if (uri.scheme != "content") return null
        if (!android.provider.DocumentsContract.isDocumentUri(context, uri)) return null
        val docId = android.provider.DocumentsContract.getDocumentId(uri)
        val split = docId.split(":", limit = 2)
        val type = split[0]
        val rel = if (split.size > 1) split[1] else ""
        when (uri.authority) {
            "com.android.externalstorage.documents" -> {
                if ("primary".equals(type, ignoreCase = true)) {
                    "${android.os.Environment.getExternalStorageDirectory()}/$rel"
                } else {
                    "/storage/$type/$rel"
                }
            }
            else -> null
        }
    }.getOrNull()
    if (real != null) {
        val f = File(real)
        if (f.isFile) return f
    }
    return copyPickedGame(context, uri)
}

internal fun copyPickedGame(context: Context, uri: Uri): File? {
    return runCatching {
        val name = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "game.exe"
    // Land the copy on shared storage (Downloads/WinHub): app-private cacheDir is invisible
    // inside the container chroot, which left the shortcut's Exec path unresolvable.
    val preferred = File(
        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
        "WinHub",
    )
    val dir = if (preferred.isDirectory || preferred.mkdirs()) preferred
        else File(context.cacheDir, "hub_pick").apply { mkdirs() }
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
@OptIn(ExperimentalFoundationApi::class)
internal fun HubCarousel(
    entries: List<HubEntry>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onMenu: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return
    val current = selected.coerceIn(0, entries.size - 1)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.ButtonStart || event.key == Key.ButtonSelect || event.key == Key.Menu)
                ) {
                    onMenu(current)
                    return@onPreviewKeyEvent true
                }
                false
            }
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
        // One infinite clock for the selected cover's stroke: blue -> light blue, sliding around
        // the frame while the tile is focused.
        val shimmer = rememberInfiniteTransition(label = "hubStroke")
        val strokePhase by shimmer.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing)),
            label = "hubStrokePhase",
        )
        entries.forEachIndexed { index, entry ->
            val delta = index - current
            val distance = abs(delta)
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
                    .then(
                        if (isSelected) {
                            Modifier.drawWithContent {
                                drawContent()
                                val w = 4.dp.toPx()
                                val span = size.width + size.height
                                val x = -span + span * 2f * strokePhase
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF1A9FFF),
                                            Color(0xFFA6E3FF),
                                            Color(0xFF1A9FFF),
                                        ),
                                        start = Offset(x, 0f),
                                        end = Offset(x + span, size.height),
                                    ),
                                    topLeft = Offset(w / 2f, w / 2f),
                                    size = Size(size.width - w, size.height - w),
                                    cornerRadius = CornerRadius(10.dp.toPx() - w / 2f),
                                    style = Stroke(width = w),
                                )
                            }
                        } else {
                            Modifier.border(1.dp, DeckPalette.line, RoundedCornerShape(10.dp))
                        },
                    )
                    .combinedClickable(
                        onClick = { onSelect(index) },
                        onLongClick = { onMenu(index) },
                    ),
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
                // Title + where it came from, burned into the top of the cover.
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.74f),
                                1f to Color.Transparent,
                            ),
                        )
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = entry.title,
                        color = Color.White,
                        fontSize = if (isSelected) 13.sp else 11.sp,
                        lineHeight = if (isSelected) 15.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = hubOriginLabel(entry).uppercase(),
                        color = Color(0xFF8FD4FF),
                        fontSize = if (isSelected) 9.sp else 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.1.sp,
                        maxLines = 1,
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

/** "Steam" / "Epic" / "GOG" / "Amazon" for store-added games, "Custom" for hand-added ones. */
private fun hubOriginLabel(entry: HubEntry): String =
    entry.game?.store?.takeIf { it.isNotBlank() }
        ?: if (entry.game?.appId != null) "Steam" else "Custom"

// ── Where an added game lives: an existing container (Master) vs its own clone (Cronos) ──────────
internal enum class HubAddMode { MASTER, CRONOS }

/**
 * The deck-styled "where should this game live?" menu the moment Add Games is used: Master
 * Containers (a shortcut inside a container you already have) vs Cronos (a fresh dedicated
 * container built just for this game). Picking a mode opens the system file picker.
 */
@Composable
internal fun DeckAddModeMenu(onPick: (HubAddMode) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .clip(DeckPalette.groupShape)
                .background(DeckPalette.surface)
                .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
                .padding(22.dp),
        ) {
            Text(
                "Add a game",
                color = DeckPalette.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Where should it live?",
                color = DeckPalette.onSurfaceVariant,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
            DeckAddModeCard(
                title = "Master Containers",
                desc = "Add it as a shortcut inside a container you already have.",
                icon = Icons.Filled.Layers,
            ) { onPick(HubAddMode.MASTER) }
            Spacer(Modifier.height(10.dp))
            DeckAddModeCard(
                title = "Cronos",
                desc = "A fresh dedicated container built just for this game — one game, one container.",
                icon = Icons.Filled.SportsEsports,
            ) { onPick(HubAddMode.CRONOS) }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel", color = DeckPalette.onSurfaceVariant)
            }
        }
    }
}

/** One big gradient-edged card for [DeckAddModeMenu]. */
@Composable
private fun DeckAddModeCard(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(DeckPalette.primary.copy(alpha = 0.14f), DeckPalette.surfaceVariant),
                ),
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(DeckPalette.primary.copy(alpha = 0.85f), DeckPalette.primary.copy(alpha = 0.25f)),
                ),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DeckPalette.primary.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = DeckPalette.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = DeckPalette.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                desc,
                color = DeckPalette.onSurfaceVariant,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DeckPalette.onSurfaceVariant)
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
    mode: HubAddMode = HubAddMode.MASTER,
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
                DeckConfigTextField("Game name", name, { name = it })

                if (mode == HubAddMode.CRONOS) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DeckPalette.primary.copy(alpha = 0.12f))
                            .border(1.dp, DeckPalette.primary.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = DeckPalette.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Cronos — a fresh dedicated container named after this game, with just this game's shortcut.",
                            color = DeckPalette.onSurfaceVariant,
                            fontSize = 11.5.sp,
                        )
                    }
                } else if (containerNames.isNotEmpty()) {
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
                        onConfirm(name.trim().ifEmpty { file.nameWithoutExtension }, appId, if (mode == HubAddMode.CRONOS) -1 else containerIdx, coverPath)
                    }
                }
            }
        }
    }
}

// ── Deck-styled launch-method menu + the SteamLite/Goldberg launch pipeline behind it ──────────────
// Steam-origin games get SteamLite (real client) / Goldberg (offline emulator) / Raw .exe; every
// other source is Raw-only — the same source gates as the old UI's LaunchMethodSheet, restyled.
@Composable
internal fun DeckLaunchMethodDialog(
    shortcut: Shortcut,
    onDismiss: () -> Unit,
    onLaunch: (mode: String, goldbergMode: GoldbergMode?, remember: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val isSteam = remember(shortcut) { isSteamOriginShortcut(shortcut) }
    val appId = remember(shortcut) { steamAppIdOf(shortcut) }
    SteamPrefs.init(context)
    var method by remember(shortcut) {
        mutableStateOf(
            when (shortcut.getExtra("launchMode", "")) {
                "Goldberg" -> "Goldberg"
                "Raw" -> "Raw"
                "RealSteam" -> "RealSteam"
                else -> if (isSteam) "RealSteam" else "Raw"
            }.let { if (isSteam) it else "Raw" },
        )
    }
    var goldbergMode by remember(shortcut) {
        mutableStateOf(SteamPrefs.getGoldbergMode(appId).let { if (it == GoldbergMode.OFF) GoldbergMode.REGULAR else it })
    }
    var rememberChoice by remember(shortcut) { mutableStateOf(shortcut.getExtra("launchModeRemembered", "") == "1") }

    val methods = remember(isSteam) {
        if (isSteam) listOf(
            Triple("RealSteam", "SteamLite", "Real Steam client - online, VAC, cloud saves"),
            Triple("Goldberg", "Goldberg", "Offline Steam emulator - no login"),
            Triple("Raw", "Raw .exe", "Run the game directly - no Steam layer"),
        ) else listOf(
            Triple("Raw", "Raw .exe", "Run the game directly - no Steam layer"),
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.92f)
                .clip(DeckPalette.groupShape)
                .background(DeckPalette.surface)
                .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
                .padding(22.dp),
        ) {
            Text(
                shortcut.name,
                color = DeckPalette.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Choose how to launch",
                color = DeckPalette.onSurfaceVariant,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
            methods.forEach { (mode, title, desc) ->
                val picked = method == mode
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (picked) DeckPalette.primary.copy(alpha = 0.10f) else DeckPalette.surfaceVariant)
                        .border(if (picked) 1.5.dp else 1.dp, if (picked) DeckPalette.primary else DeckPalette.line, RoundedCornerShape(14.dp))
                        .clickable { method = mode }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (picked) DeckPalette.primary else DeckPalette.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            color = if (picked) DeckPalette.primary else DeckPalette.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            desc,
                            color = DeckPalette.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                    if (picked) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = DeckPalette.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { rememberChoice = !rememberChoice }
                    .padding(vertical = 10.dp),
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (rememberChoice) DeckPalette.primary else Color.Transparent)
                        .border(1.dp, if (rememberChoice) DeckPalette.primary else DeckPalette.line, RoundedCornerShape(6.dp)),
                ) {
                    if (rememberChoice) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = DeckPalette.onPrimary,
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.Center),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Remember this choice",
                    color = DeckPalette.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            Spacer(Modifier.height(14.dp))
            HubGradientButton(
                "Launch",
                onClick = { onLaunch(method, if (method == "Goldberg") goldbergMode else null, rememberChoice) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel", color = DeckPalette.onSurfaceVariant)
            }
        }
    }
}

/**
 * The launch pipeline behind the deck menu: Goldberg (patch-then-launch, component on demand),
 * SteamLite (component on demand → Steam pre-flight) and Raw (direct), mirroring BigPictureScreen's
 * flow with deck-styled progress chrome. [request] non-null opens the method menu for that shortcut.
 */
@Composable
internal fun DeckLaunchStack(activity: Activity, request: Shortcut?, onMenuClosed: () -> Unit) {
    val context = LocalContext.current
    var preflightFor by remember { mutableStateOf<Shortcut?>(null) }
    var componentDownloadFor by remember { mutableStateOf<Shortcut?>(null) }
    var componentDownloadLabel by remember { mutableStateOf("") }
    var componentDownloadProgress by remember { mutableStateOf(0f) }

    fun launchWithGoldberg(s: Shortcut, gm: GoldbergMode) {
        val appId = steamAppIdOf(s)
        SteamPrefs.init(context)
        SteamPrefs.setGoldbergMode(appId, gm)
        val applyThenLaunch = {
            Thread({
                val installDir = runCatching {
                    SteamDatabase.getInstance(context).getGame(appId)?.installDir
                }.getOrNull().orEmpty()
                activity.runOnUiThread {
                    if (installDir.isEmpty()) {
                        launchShortcut(activity, s)
                    } else {
                        GoldbergPatcher.applyModeAsync(context, appId, installDir, s.name, gm) { _, _ ->
                            launchShortcut(activity, s)
                        }
                    }
                }
            }, "deck-goldberg-apply-launch").start()
        }
        if (!GoldbergComponent.isInstalled(context)) {
            componentDownloadFor = s
            componentDownloadLabel = "Steam Emulator (Goldberg)"
            componentDownloadProgress = 0f
            GoldbergComponent.downloadAsync(
                context,
                { f -> componentDownloadProgress = f },
                { ok, msg ->
                    componentDownloadFor = null
                    if (ok) applyThenLaunch()
                    else Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
            )
        } else applyThenLaunch()
    }

    fun launchWithSteamLite(s: Shortcut) {
        if (!SteamLiteComponent.isInstalled(context)) {
            componentDownloadFor = s
            componentDownloadLabel = "SteamLite (Real Steam / VAC)"
            componentDownloadProgress = 0f
            SteamLiteComponent.downloadAsync(
                context,
                { f -> componentDownloadProgress = f },
                { ok, msg ->
                    componentDownloadFor = null
                    if (ok) preflightFor = s
                    else Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
            )
        } else {
            preflightFor = s
        }
    }

    request?.let { s ->
        DeckLaunchMethodDialog(
            shortcut = s,
            onDismiss = onMenuClosed,
            onLaunch = { mode, gm, rememberChoice ->
                onMenuClosed()
                s.putExtra("launchMode", mode)
                s.putExtra("launchModeRemembered", if (rememberChoice) "1" else "0")
                s.saveData()
                when (mode) {
                    "Goldberg" -> launchWithGoldberg(s, gm ?: GoldbergMode.REGULAR)
                    "Raw" -> launchShortcut(activity, s)
                    else -> launchWithSteamLite(s)
                }
            },
        )
    }

    componentDownloadFor?.let {
        Dialog(onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(
                Modifier
                    .fillMaxWidth(0.85f)
                    .clip(DeckPalette.groupShape)
                    .background(DeckPalette.surface)
                    .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
                    .padding(22.dp),
            ) {
                Text(
                    "Downloading $componentDownloadLabel",
                    color = DeckPalette.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(DeckPalette.surfaceVariant),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(componentDownloadProgress.coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(DeckPalette.primary),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "${(componentDownloadProgress.coerceIn(0f, 1f) * 100).toInt()}% - the game launches when this finishes.",
                    color = DeckPalette.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }

    preflightFor?.let { s ->
        val appId = steamAppIdOf(s)
        val installDir = remember(s) {
            runCatching { SteamDatabase.getInstance(context).getGame(appId)?.installDir }.getOrNull().orEmpty()
        }
        val savePrefs = remember { context.getSharedPreferences("save_manager_prefs", Context.MODE_PRIVATE) }
        SteamPreflightDialog(
            shortcut = s,
            request = SteamSessionManager.PreflightRequest(
                appId = appId,
                installDir = installDir,
                gameName = s.name,
                pullCloudSaves = savePrefs.getBoolean("auto_download_steam_on_launch", true),
            ),
            onLaunch = { preflightFor = null; launchShortcut(activity, s, preflightDone = true) },
            onDismiss = { preflightFor = null },
            onSignIn = {
                preflightFor = null
                context.startActivity(Intent(context, SteamLoginActivity::class.java))
            },
            onGoldberg = {
                preflightFor = null
                SteamPrefs.init(context)
                launchWithGoldberg(s, SteamPrefs.getGoldbergMode(appId).let { if (it == GoldbergMode.OFF) GoldbergMode.REGULAR else it })
            },
            onUpdate = { _ -> preflightFor = null },
        )
    }
}

/**
 * The deck-styled game menu: long-press a carousel tile (touch) or press Options/Select (controller).
 * Play / Settings (opens this game's container editor) / Add to Home Screen / Properties / Delete.
 */
@Composable
internal fun DeckGameMenu(
    title: String,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
    onAddToHomeScreen: () -> Unit,
    onProperties: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.78f)
                .clip(DeckPalette.groupShape)
                .background(DeckPalette.surface)
                .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
                .padding(vertical = 10.dp),
        ) {
            Text(
                title,
                color = DeckPalette.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
            DeckGameMenuRow("Play", Icons.Filled.PlayArrow, onClick = onPlay)
            DeckGameMenuRow("Settings", Icons.Filled.Settings, onClick = onSettings)
            DeckGameMenuRow("Add to Home Screen", Icons.Filled.Home, onClick = onAddToHomeScreen)
            DeckGameMenuRow("Properties", Icons.Filled.Info, onClick = onProperties)
            DeckGameMenuRow("Delete", Icons.Filled.Delete, danger = true, onClick = onDelete)
        }
    }
}

@Composable
private fun DeckGameMenuRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (danger) DeckPalette.error else DeckPalette.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (danger) DeckPalette.error else DeckPalette.onBackground, fontSize = 14.sp)
    }
}

/**
 * Post-create Cronos check: the key launch-relevant settings of the container that was just
 * built (wine, graphics driver, DX wrapper + versions, Box64/FEXCore preset, screen, audio) so
 * the user can confirm them or jump straight into the container editor.
 */
@Composable
internal fun DeckCronosReviewDialog(
    container: com.winlator.star.container.Container,
    onKeep: () -> Unit,
    onEdit: () -> Unit,
) {
    val cfg = remember(container) { runCatching { container.getDXWrapperConfig() }.getOrNull() ?: "" }
    val dxvkVer = remember(cfg) { Regex("""(?:^|,)version=([^,]+)""").find(cfg)?.groupValues?.get(1) }
    val vkd3dVer = remember(cfg) { Regex("""(?:^|,)vkd3dVersion=([^,]+)""").find(cfg)?.groupValues?.get(1) }
    // Every value is coerced to a non-null String: a container whose config predates a field (or
    // was created without a defaults profile) hands back null, and Text(null) NPEs the moment this
    // window composes — which is exactly how the post-create Cronos check crashed the app.
    val rows = remember(container, dxvkVer, vkd3dVer) {
        listOf(
            "Wine" to (runCatching { container.wineVersion }.getOrNull() ?: "?"),
            "Graphics driver" to (runCatching { container.graphicsDriver }.getOrNull() ?: "?"),
            "DX wrapper" to buildString {
                append(runCatching { container.getDXWrapper() }.getOrNull() ?: "?")
                dxvkVer?.let { append(" (dxvk ") ; append(it); append(")") }
                vkd3dVer?.let { append(" / vkd3d ") ; append(it) }
            },
            "Emulator" to (runCatching { container.emulator }.getOrNull() ?: "?"),
            "Box64 preset" to (runCatching { container.box64Preset }.getOrNull() ?: "-"),
            "FEXCore preset" to (runCatching { container.getFEXCorePreset() }.getOrNull() ?: "-"),
            "Screen size" to (runCatching { container.screenSize }.getOrNull() ?: "?"),
            "Audio" to (runCatching { container.audioDriver }.getOrNull() ?: "?"),
        )
    }

    Dialog(onDismissRequest = onKeep, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.88f)
                .clip(DeckPalette.groupShape)
                .background(DeckPalette.surface)
                .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
                .padding(22.dp),
        ) {
            Text(
                "Cronos created - are these settings right?",
                color = DeckPalette.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                runCatching { container.name }.getOrNull() ?: "",
                color = DeckPalette.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
            )
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(
                        label,
                        color = DeckPalette.onSurfaceVariant,
                        fontSize = 12.5.sp,
                        modifier = Modifier.width(130.dp),
                    )
                    Text(
                        value,
                        color = DeckPalette.onBackground,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HubGradientButton("Settings are good", onClick = onKeep, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Text("Edit settings", color = DeckPalette.onSurfaceVariant)
            }
        }
    }
}
