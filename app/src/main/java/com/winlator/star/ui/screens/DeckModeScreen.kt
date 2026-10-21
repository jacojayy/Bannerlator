package com.winlator.star.ui.screens

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.widget.Toast
import android.widget.VideoView

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.coroutineScope
import kotlin.math.roundToInt
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.winlator.star.R
import com.winlator.star.XServerDisplayActivity
import com.winlator.star.container.Container
import com.winlator.star.contents.ContentProfile
import com.winlator.star.contents.ContentsManager
import com.winlator.star.container.ContainerManager
import com.winlator.star.container.Shortcut
import com.winlator.star.core.GameIdentifier
import com.winlator.star.store.SaveManagerScreen
import com.winlator.star.store.deckTileFocus
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.FileOutputStream
import com.winlator.star.ui.ControllerFocusIndication
import com.winlator.star.ui.SocialContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The DroidDeck palette (Graphite), lifted verbatim from DroidDeck's ui/Theme.kt. */
internal object Deck {
    val background = Color(0xFF0A0B0D)
    val surface = Color(0xFF121417)
    val surfaceVariant = Color(0xFF1A1D22)
    val line = Color(0xFF262A31)
    val onBackground = Color(0xFFF2F4F7)
    val onSurfaceVariant = Color(0xFF9AA3AF)
    val primary = Color(0xFF1A9FFF)
    val onPrimary = Color(0xFF03111F)
    val attention = Color(0xFFFFB547)
}

/** One tile on the wall: a name (drives its colour), the resolved hue, and its cover art. */
internal class DeckCover(val name: String, val hue: Float, val bitmap: Bitmap? = null)

internal fun hueOf(name: String): Float {
    var h = 0
    for (c in name) h = 31 * h + c.code
    return ((h % 360) + 360) % 360 / 360f
}

/** The rail's real sections — the app's own destinations, in the DroidDeck rail style. */
private class DeckSection(val key: String, val label: String, val icon: ImageVector)

private fun deckSections() = listOf(
    DeckSection("home", "Home", Icons.Filled.Apps),
    DeckSection("containers", "Storage", Icons.Filled.Folder),
    DeckSection("stores", "Stores", Icons.Filled.Storefront),
    DeckSection("file_manager", "Files", Icons.Filled.FolderOpen),
    DeckSection("input_controls", "Controls", Icons.Filled.SportsEsports),
    DeckSection("contents", "Contents", Icons.Filled.Inventory2),
    DeckSection("saves", "Saves", Icons.Filled.Save),
    DeckSection("social_hub", "Social", Icons.Filled.People),
    DeckSection("settings", "Settings", Icons.Filled.Settings),
)

/**
 * Deck Mode — DroidDeck's front end, full-bleed (MainActivity drops the top bar on the `deck`
 * route). The rail lists this app's own sections; each one hosts the REAL screen, so Deck Mode
 * is a shell over working functionality rather than a mock.
 */
@Composable
internal fun DeckModeScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    var introDone by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf("home") }
    var graphicPane by remember { mutableStateOf("gpu") }
    var deckDetail by remember { mutableStateOf<Int?>(null) }

    // Controller-first back stack: B (or system Back) closes the container editor, then returns to
    // Home from any tab, then leaves Deck Mode.
    val goBack: () -> Unit = {
        when {
            deckDetail != null -> deckDetail = null
            section != "home" -> section = "home"
            else -> onExit()
        }
    }
    BackHandler { goBack() }

    // The pump sits on the current rail item the moment the New UI opens, so the controller starts
    // on the side bar — then Right/A drops into the pane and the tab drags in behind it.
    val railFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val tabs = remember { DeckTabHost() }
    val backLayer = remember { DeckBackLayer() }
    val editTracker = remember { DeckEditTracker() }
    LaunchedEffect(section, deckDetail) { tabs.bind(0) }
    LaunchedEffect(introDone, section) {
        if (introDone) runCatching { railFocus.requestFocus() }
    }
    // Back from a detail re-parks focus on the rail item we came from. Opening a detail parks
    // focus on its own Back button (see DeckContainerDetailScreen), never on an invisible node.
    LaunchedEffect(deckDetail) {
        if (deckDetail == null && introDone) runCatching { railFocus.requestFocus() }
    }

    // Truly full screen: the system status/nav bars go with the top bar, restored on leaving.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        if (controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            previousOrientation?.let { activity?.requestedOrientation = it }
        }
    }

    // DroidDeck's Graphite palette as the theme for everything hosted here, so no Amethyst
    // purple reaches a hosted screen — and the focus outline follows the same blue.
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Deck.primary,
            onPrimary = Deck.onPrimary,
            secondary = Deck.onSurfaceVariant,
            background = Deck.background,
            onBackground = Deck.onBackground,
            surface = Deck.surface,
            onSurface = Deck.onBackground,
            surfaceVariant = Deck.surfaceVariant,
            onSurfaceVariant = Deck.onSurfaceVariant,
            error = Color(0xFFFF8A80),
        ),
    ) {
        CompositionLocalProvider(
            LocalIndication provides remember { ControllerFocusIndication(Deck.primary) },
            LocalDeckTabs provides tabs,
            LocalDeckBack provides backLayer,
            LocalDeckEditTracker provides editTracker,
            LocalDeckMode provides true,
        ) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.ButtonB -> {
                        // A text field that is taking input owns B first (it just closes the IME).
                        if (editTracker.editing) false
                        else if (backLayer.onBack?.invoke() == true) true
                        else if (tabs.onBack?.invoke() == true) true
                        else { goBack(); true }
                    }
                    // Shoulders are just wide horizontal steps. They never change the rail or a tab.
                    Key.ButtonL1 -> focusManager.moveFocus(FocusDirection.Left)
                    Key.ButtonR1 -> focusManager.moveFocus(FocusDirection.Right)
                    else -> false
                }
            },
    ) {
        DeckSideRail(
            selected = section,
            focusRequester = railFocus,
            onSelect = { next ->
                section = next
                deckDetail = null
            },
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds(),
        ) {
            // Every tab switch slides the incoming pane up from the bottom while the outgoing one
            // drops away — one motion for the whole shell.
            AnimatedContent(
                targetState = section,
                transitionSpec = {
                    (fadeIn(tween(300, delayMillis = 80, easing = DeckEase)) +
                        slideInVertically(tween(420, delayMillis = 80, easing = DeckEase)) { it / 24 })
                        .togetherWith(
                            fadeOut(tween(170, easing = DeckEase)) +
                                slideOutVertically(tween(170, easing = DeckEase)) { -it / 40 },
                        )
                },
                modifier = Modifier.fillMaxSize(),
                label = "deckSection",
            ) { currentSection ->
                when (currentSection) {
                    "home" -> DeckHome(onEditContainer = { id -> section = "containers"; deckDetail = id })
                    "containers" -> AnimatedContent(
                        targetState = deckDetail,
                        transitionSpec = {
                            (fadeIn(tween(300, delayMillis = 80, easing = DeckEase)) +
                                slideInVertically(tween(420, delayMillis = 80, easing = DeckEase)) { it / 24 })
                                .togetherWith(
                                    fadeOut(tween(170, easing = DeckEase)) +
                                        slideOutVertically(tween(170, easing = DeckEase)) { -it / 40 },
                                )
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "deckDetail",
                    ) { detailId ->
                        if (detailId != null) {
                            DeckContainerDetailScreen(
                                containerId = detailId,
                                onNavigateBack = { deckDetail = null },
                            )
                        } else {
                            DeckContainersScreen(onEditContainer = { id -> deckDetail = id })
                        }
                    }
                    "stores" -> DeckStoresScreen()
                    "file_manager" -> FileManagerScreen(deckMode = true)
                    "graphics" -> DeckGraphicsPane(
                        pane = graphicPane,
                        onSelect = { graphicPane = it },
                    )
                    "input_controls" -> InputControlsScreen()
                    "contents" -> DeckContentsScreen()
                    // The full Save Manager (Steam / Custom / Epic / GOG / Settings), in Deck chrome.
                    "saves" -> SaveManagerScreen(deckMode = true)
                    "social_hub" -> SocialContent()
                    "settings" -> DeckSettingsScreen()
                    else -> DeckHome(onEditContainer = { id -> section = "containers"; deckDetail = id })
                }
            }

            // Controller legend: the one place a player can look to know what L1/R1 do here.
            // Draw-only text, so it never becomes a focus stop.
            Text(
                text = "A Select   B Back   L1/R1 Move   Stick / D-pad Move",
                color = Deck.onSurfaceVariant.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 8.dp),
            )

            // DroidDeck's corner light — one circular blue glow just inside the top right of every
            // pane. Draw-only, so it never intercepts touches meant for the screen underneath, and
            // anchored so it stops short of the pane's content instead of washing across it.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val radius = 460.dp.toPx()
                        val center = Offset(size.width - radius * 0.35f, radius * 0.35f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Deck.primary.copy(alpha = 0.24f),
                                    Deck.primary.copy(alpha = 0.10f),
                                    Deck.primary.copy(alpha = 0.04f),
                                    Color.Transparent,
                                ),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                    },
            )
        }
    }
        if (!introDone) {
            DeckIntro(onDone = { introDone = true })
        }
        }
    }
}

/** The featured library: bundled SteamGridDB vertical grids under assets/covers. */
internal val featuredCovers = listOf(
    "Red Dead Redemption 2" to "covers/red_dead_redemption_2.jpg",
    "Red Dead Redemption" to "covers/red_dead_redemption.jpg",
    "Cyberpunk 2077" to "covers/cyberpunk_2077.jpg",
    "Hogwarts Legacy" to "covers/hogwarts_legacy.jpg",
    "High on Life" to "covers/high_on_life.jpg",
    "High on Life 2" to "covers/high_on_life_2.jpg",
    "Dinkum" to "covers/dinkum.jpg",
    "Titanfall 2" to "covers/titanfall_2.jpg",
    "Terraria" to "covers/terraria.jpg",
    "Star Wars Jedi: Fallen Order" to "covers/star_wars_jedi_fallen_order.jpg",
    "Marvel's Spider-Man 2" to "covers/spider_man_2.jpg",
    "Marvel's Spider-Man Remastered" to "covers/spider_man_remastered.jpg",
    "Marvel's Spider-Man: Miles Morales" to "covers/spider_man_miles_morales.jpg",
    "Ratchet & Clank: Rift Apart" to "covers/ratchet_and_clank_rift_apart.jpg",
                            "Assassin's Creed IV: Black Flag" to "covers/assassins_creed_4_black_flag.jpg",
                        "Assassin's Creed Odyssey" to "covers/assassins_creed_odyssey.jpg",
    "Assassin's Creed Valhalla" to "covers/assassins_creed_valhalla.jpg",
    "Assassin's Creed Mirage" to "covers/assassins_creed_mirage.jpg",
                )

/** Decode a bundled cover at roughly capsule resolution so the wall stays light on RAM. */
/** Best-effort store tag for a hand-added exe, so home doesn't call every Steam game "Custom". */
private fun hubStoreFromPath(path: String): String? {
    val p = path.lowercase()
    return when {
        p.contains("steamapps") || p.contains("steam_games") || p.contains("/steam/") -> "Steam"
        p.contains("epic games") -> "Epic"
        p.contains("gog games") || p.contains("gog galaxy") -> "GOG"
        p.contains("amazon games") -> "Amazon"
        else -> null
    }
}

internal fun decodeCover(context: Context, asset: String): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.assets.open(asset).use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 320) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.assets.open(asset).use { BitmapFactory.decodeStream(it, null, opts) }
}.getOrNull()

/** Home: the cover wall behind a bottom-anchored 3D carousel — WinHub wordmark, Add Games. */
@Composable
private fun DeckHome(onEditContainer: (Int) -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    var covers by remember { mutableStateOf<List<DeckCover>>(emptyList()) }
    var entries by remember { mutableStateOf<List<HubEntry>?>(null) }
    var containers by remember { mutableStateOf<List<Container>>(emptyList()) }
    var selected by remember { mutableStateOf(0) }
    var pending by remember { mutableStateOf<Pair<File, GameIdentifier.GameIdentity>?>(null) }
    var launchRequest by remember { mutableStateOf<Shortcut?>(null) }
    var launchSteam by remember { mutableStateOf<Boolean?>(null) }
    var pageFor by remember { mutableStateOf<HubEntry?>(null) }
    var addModeMenu by remember { mutableStateOf(false) }
    var addMode by remember { mutableStateOf(HubAddMode.MASTER) }
    var cronosBusy by remember { mutableStateOf(false) }
    var cronosName by remember { mutableStateOf("") }
    var gameMenuFor by remember { mutableStateOf<HubEntry?>(null) }
    var propertiesFor by remember { mutableStateOf<Container?>(null) }
    var cronosReviewFor by remember { mutableStateOf<Container?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            covers = featuredCovers.map { (name, asset) ->
                DeckCover(name, hueOf(name), decodeCover(context, asset))
            }
            entries = HubGamesStore.load(context).map { g ->
                HubEntry(g.name, g.coverPath?.let { BitmapFactory.decodeFile(it) }, g)
            }
            val loaded = entries.orEmpty()
            val lastPlayed = context.getSharedPreferences("hub_prefs", Context.MODE_PRIVATE)
                .getString("last_played", null)
            val lastIndex = loaded.indexOfFirst { it.game?.shortcutPath == lastPlayed }
            selected = if (lastIndex >= 0) lastIndex else (loaded.size - 1).coerceAtLeast(0)
            containers = ContainerManager(context).also { it.reloadContainers() }.getContainers().toList()
        }
    }

    val lastPlayedPath = context.getSharedPreferences("hub_prefs", Context.MODE_PRIVATE)
        .getString("last_played", null)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val file = resolvePickedGame(context, uri)
            val identity = file?.let { runCatching { GameIdentifier.identify(it) }.getOrNull() }
            withContext(Dispatchers.Main) {
                if (file == null) {
                    Toast.makeText(context, "Couldn't read that file", Toast.LENGTH_SHORT).show()
                } else {
                    pending = file to (identity ?: GameIdentifier.GameIdentity())
                }
            }
        }
    }

    fun playEntry(entry: HubEntry) {
        val game = entry.game
        if (game == null) {
            Toast.makeText(context, "Add this game first — use Add Games", Toast.LENGTH_SHORT).show()
            return
        }
        context.getSharedPreferences("hub_prefs", Context.MODE_PRIVATE)
            .edit().putString("last_played", game.shortcutPath).apply()
        selected = (entries.orEmpty().indexOfFirst { it.game?.shortcutPath == game.shortcutPath })
            .takeIf { it >= 0 } ?: selected
        launchSteam = game.appId != null || game.store == "Steam"
        scope.launch(Dispatchers.IO) {
            val shortcut = runCatching {
                val container = containers.find { it.id == game.containerId } ?: return@runCatching null
                val desktop = File(game.shortcutPath)
                if (!desktop.isFile) return@runCatching null
                Shortcut(container, desktop)
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (shortcut == null) {
                    Toast.makeText(context, "That game's shortcut is missing — re-add it", Toast.LENGTH_SHORT).show()
                } else {
                    launchRequest = shortcut
                }
            }
        }
    }

    val backLayer = LocalDeckBack.current
    DisposableEffect(pageFor) {
        backLayer.onBack = if (pageFor != null) {
            { pageFor = null; true }
        } else {
            null
        }
        onDispose { backLayer.onBack = null }
    }

    val wallCovers = remember(covers) {
        if (covers.isEmpty()) List(12) { index -> DeckCover("", index / 12f) } else covers
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .onPreviewKeyEvent { e ->
                // Y is the game's options, the controller's answer to a long-press.
                if (e.type == KeyEventType.KeyDown && e.key == Key.ButtonY) {
                    val focused = entries?.getOrNull(selected) ?: return@onPreviewKeyEvent false
                    gameMenuFor = focused
                    true
                } else {
                    false
                }
            }
            .clipToBounds(),
    ) {
        DeckCapsuleWall(
            covers = wallCovers,
            driftMs = if (covers.isEmpty()) 75_000 else 40_000,
        )
        DeckWallFade()

        // ── Home, bannercode deck style: a hero for what you're playing, then shelves. ──
        entries?.let { list ->
            if (list.isEmpty()) {
                Text(
                    "No games yet — tap +",
                    color = DeckPalette.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                // Continue playing is only what has actually been played (or the last launched
                // shortcut); a freshly added game must not appear here.
                val continuePlaying = remember(list) {
                    val last = context.getSharedPreferences("hub_prefs", Context.MODE_PRIVATE)
                        .getString("last_played", null)
                    orderedForHome(list, context).filter { entry ->
                        hubHasPlaytime(context, entry.title) ||
                            (last != null && entry.game?.shortcutPath == last)
                    }
                }
                // Recently added only means the last week of additions; legacy entries (no stamp)
                // stay out so this shelf never becomes a copy of Installed.
                // A game already shown above must not reappear in the shelf below it: no card is
                // ever on screen twice (Continue playing wins, then Installed, then Recently added).
                val continuePaths = remember(continuePlaying) {
                    continuePlaying.mapNotNull { it.game?.shortcutPath }.toSet()
                }
                val installed = remember(list, continuePaths) {
                    list.filterNot { entry -> entry.game?.shortcutPath?.let { it in continuePaths } == true }
                }
                val recentlyAdded = remember(list, continuePaths) {
                    val weekAgo = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
                    list.filter { entry ->
                        val stamp = entry.game?.addedAt ?: 0L
                        entry.game?.shortcutPath?.let { it in continuePaths } != true &&
                            stamp > 0L && stamp >= weekAgo
                    }.sortedByDescending { it.game?.addedAt ?: 0L }.take(8)
                }
                val hero = list.getOrNull(selected) ?: continuePlaying.firstOrNull()
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 24.dp, end = 24.dp, top = 76.dp, bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (hero != null) {
                        item(key = "hero") {
                            DeckHomeHero(
                                entry = hero,
                                playtime = hubPlaytimeLabel(context, hero.title),
                                isContinue = hero.game?.shortcutPath == lastPlayedPath,
                                onOpen = { pageFor = hero },
                                onPlay = { playEntry(hero) },
                                onMenu = { gameMenuFor = hero },
                            )
                        }
                    }
                    if (continuePlaying.isNotEmpty()) {
                        item(key = "shelf-continue") {
                            DeckHomeShelf(
                                title = "Continue playing",
                                entries = continuePlaying.take(8),
                                wide = true,
                                context = context,
                                onOpen = { pageFor = it },
                                onMenu = { gameMenuFor = it },
                                onFocus = { selected = list.indexOf(it) },
                            )
                        }
                    }
                    if (installed.isNotEmpty()) {
                        item(key = "shelf-installed") {
                            DeckHomeShelf(
                                title = "Installed",
                                entries = installed,
                                wide = false,
                                context = context,
                                onOpen = { pageFor = it },
                                onMenu = { gameMenuFor = it },
                                onFocus = { selected = list.indexOf(it) },
                            )
                        }
                    }
                    if (recentlyAdded.isNotEmpty()) {
                        item(key = "shelf-recent") {
                            DeckHomeShelf(
                                title = "Recently added",
                                entries = recentlyAdded,
                                wide = false,
                                context = context,
                                onOpen = { pageFor = it },
                                onMenu = { gameMenuFor = it },
                                onFocus = { selected = list.indexOf(it) },
                            )
                        }
                    }
                }
            }
        }

        // Add Games is just a + in the corner now.
        val addInteraction = remember { MutableInteractionSource() }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 18.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(Deck.surface)
                .deckFocusBorder(addInteraction, CircleShape)
                .deckBringIntoView()
                .clickable(
                    interactionSource = addInteraction,
                    indication = null,
                ) { addModeMenu = true },
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add games",
                tint = Deck.onBackground,
                modifier = Modifier.size(22.dp),
            )
        }

        if (addModeMenu) {
            DeckAddModeMenu(
                onPick = { mode ->
                    addMode = mode
                    addModeMenu = false
                    picker.launch(arrayOf("*/*"))
                },
                onDismiss = { addModeMenu = false },
            )
        }

        if (cronosBusy) {
            Dialog(onDismissRequest = { }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Column(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .clip(DeckPalette.groupShape)
                        .background(DeckPalette.surface)
                        .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = DeckPalette.primary)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Building Cronos container\n$cronosName…",
                        color = DeckPalette.onBackground,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        gameMenuFor?.let { entry ->
            val game = entry.game
            DeckGameMenu(
                title = game?.name ?: entry.title,
                onPlay = {
                    gameMenuFor = null
                    playEntry(entry)
                },
                onSettings = {
                    gameMenuFor = null
                    val g = entry.game ?: return@DeckGameMenu
                    if (containers.none { it.id == g.containerId }) {
                        Toast.makeText(context, "Container not found", Toast.LENGTH_SHORT).show()
                    } else {
                        onEditContainer(g.containerId)
                    }
                },
                onAddToHomeScreen = {
                    gameMenuFor = null
                    val g = entry.game ?: return@DeckGameMenu
                    val launchIntent = Intent(context, XServerDisplayActivity::class.java).apply {
                        putExtra("container_id", g.containerId)
                        putExtra("shortcut_path", g.shortcutPath)
                        putExtra("shortcut_name", g.name)
                    }
                    val bmp = entry.bitmap
                    val icon = if (bmp != null) {
                        Icon.createWithBitmap(Bitmap.createScaledBitmap(bmp, 192, 288, true))
                    } else {
                        Icon.createWithResource(context, R.mipmap.ic_launcher)
                    }
                    val info = ShortcutInfo.Builder(context, "hub_${g.containerId}_${g.name.hashCode()}")
                        .setShortLabel(g.name)
                        .setIcon(icon)
                        .setIntent(launchIntent)
                        .build()
                    val sm = context.getSystemService(ShortcutManager::class.java)
                    if (sm != null && sm.isRequestPinShortcutSupported) {
                        sm.requestPinShortcut(info, null)
                    } else {
                        Toast.makeText(context, "Pin shortcuts aren't supported here", Toast.LENGTH_SHORT).show()
                    }
                },
                onProperties = {
                    gameMenuFor = null
                    val g = entry.game ?: return@DeckGameMenu
                    propertiesFor = containers.find { it.id == g.containerId }
                    if (propertiesFor == null) Toast.makeText(context, "Container not found", Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    gameMenuFor = null
                    val g = entry.game ?: return@DeckGameMenu
                    scope.launch(Dispatchers.IO) {
                        runCatching { File(g.shortcutPath).delete() }
                        g.coverPath?.let { runCatching { File(it).delete() } }
                        HubGamesStore.remove(context, g)
                        withContext(Dispatchers.Main) {
                            val list = entries.orEmpty().filter { it.game?.shortcutPath != g.shortcutPath }
                            entries = list
                            selected = selected.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                            Toast.makeText(context, "Removed \"${g.name}\"", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = { gameMenuFor = null },
            )
        }

        propertiesFor?.let { c ->
            DeckPropertiesDialog(container = c, onDismiss = { propertiesFor = null })
        }

        cronosReviewFor?.let { c ->
            DeckCronosReviewDialog(
                container = c,
                onKeep = { cronosReviewFor = null },
                onEdit = {
                    cronosReviewFor = null
                    onEditContainer(c.id)
                },
            )
        }

        DeckLaunchStack(
            activity = activity,
            request = launchRequest,
            onMenuClosed = { launchRequest = null; launchSteam = null },
            steamOverride = launchSteam,
        )

        pageFor?.let { entry ->
            DeckHubGamePage(
                entry = entry,
                onBack = { pageFor = null },
                onPlay = { pageFor = null; playEntry(entry) },
                onSettings = {
                    val g = entry.game
                    pageFor = null
                    if (g == null || containers.none { it.id == g.containerId }) {
                        Toast.makeText(context, "Container not found", Toast.LENGTH_SHORT).show()
                    } else {
                        onEditContainer(g.containerId)
                    }
                },
            )
        }

        pending?.let { (file, identity) ->
            HubConfirmDialog(
                file = file,
                containerNames = containers.map { it.name },
                initialName = identity.name ?: file.nameWithoutExtension,
                initialAppId = identity.appId,
                mode = addMode,
                onDismiss = { pending = null },
                onConfirm = { name, appId, containerIndex, coverPath ->
                    pending = null
                    if (containerIndex < 0) {
                        // Cronos — a FRESH dedicated container with just this game's shortcut.
                        // Built through ContainerDetailViewModel (the SAME form seeding +
                        // buildCreateData the old UI's New Container FAB uses), so its config
                        // matches a hand-made container exactly. A bare Container(0).getData()
                        // cannot do that: JSONObject.put(name, null) REMOVES the key, so the null
                        // cpuList/cpuListWoW64/emulator/box64Version/fexcoreVersion never reached
                        // the config file and the container died at launch with invalid settings.
                        cronosName = name
                        cronosBusy = true
                        scope.launch(Dispatchers.IO) {
                            val mgr = ContainerManager(context).also { it.reloadContainers() }
                            val data = runCatching {
                                // Wine pick = the first entry a manual New Container offers
                                // (installed Wine profiles first, then Proton), with the editor's
                                // empty-list fallbacks so a device with no content still tries the
                                // wine a working container already uses.
                                val cm = ContentsManager(context).also { it.syncContents() }
                                val wineList =
                                    cm.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WINE)
                                        .map { ContentsManager.getEntryName(it) } +
                                        cm.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_PROTON)
                                            .map { ContentsManager.getEntryName(it) }
                                val wine = wineList.firstOrNull()
                                    ?: mgr.getContainers().firstOrNull()?.wineVersion
                                    ?: Container(0, mgr).wineVersion
                                ContainerDetailViewModel.buildFreshCreateData(context, name, wine, true)
                            }.getOrNull()
                            withContext(Dispatchers.Main) {
                                if (data == null) {
                                    cronosBusy = false
                                    Toast.makeText(context, "Cronos setup failed — couldn't build config", Toast.LENGTH_SHORT).show()
                                    return@withContext
                                }
                                mgr.createContainerAsync(data, ContentsManager(context)) { created ->
                                    cronosBusy = false
                                    if (created == null) {
                                        Toast.makeText(context, "Cronos setup failed — couldn't create the Wine prefix", Toast.LENGTH_SHORT).show()
                                        return@createContainerAsync
                                    }
                                    scope.launch(Dispatchers.IO) {
                                        val added = runCatching {
                                            val desktop = ExeShortcutImporter.addToShortcuts(context, created, file, name, appId)
                                            val finalCover = coverPath ?: appId?.let { fetchHubCover(context, it) }
                                            val game = HubGame(
                                                name, created.id, desktop.absolutePath, appId, finalCover,
                                                hubStoreFromPath(file.absolutePath),
                                            )
                                            HubGamesStore.add(context, game)
                                            HubEntry(name, finalCover?.let { BitmapFactory.decodeFile(it) }, game)
                                        }.getOrNull()
                                        containers = ContainerManager(context).also { it.reloadContainers() }.getContainers().toList()
                                        withContext(Dispatchers.Main) {
                                            if (added == null) {
                                                Toast.makeText(context, "Container created, but adding the game failed", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val list = entries.orEmpty() + added
                                                entries = list
                                                selected = list.lastIndex
                                                Toast.makeText(context, "Added \"${added.title}\" to its own Cronos container", Toast.LENGTH_SHORT).show()
                                                cronosReviewFor = created
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return@HubConfirmDialog
                    }
                    val container = containers.getOrNull(containerIndex)
                    if (container == null) {
                        Toast.makeText(context, "Create a container first", Toast.LENGTH_SHORT).show()
                        return@HubConfirmDialog
                    }
                    scope.launch(Dispatchers.IO) {
                        val added = runCatching {
                            val desktop = ExeShortcutImporter.addToShortcuts(context, container, file, name, appId)
                            val finalCover = coverPath ?: appId?.let { fetchHubCover(context, it) }
                            val game = HubGame(
                                name, container.id, desktop.absolutePath, appId, finalCover,
                                hubStoreFromPath(file.absolutePath),
                            )
                            HubGamesStore.add(context, game)
                            HubEntry(name, finalCover?.let { BitmapFactory.decodeFile(it) }, game)
                        }.getOrNull()
                        withContext(Dispatchers.Main) {
                            if (added == null) {
                                Toast.makeText(context, "Couldn't add that game", Toast.LENGTH_SHORT).show()
                            } else {
                                val list = entries.orEmpty() + added
                                entries = list
                                selected = list.lastIndex
                                Toast.makeText(context, "Added \"${added.title}\"", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
            )
        }
    }
}

/** Graphics: pick the pane, then the real screen renders below it. */
@Composable
private fun DeckGraphicsPane(pane: String, onSelect: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background),
    ) {
        when (pane) {
            "wrappers" -> WrapperManagerScreen()
            else -> AdrenoToolsScreen()
        }
        DeckStretchyTabs(
            labels = listOf("GPU drivers", "Wrappers"),
            selected = if (pane == "wrappers") 1 else 0,
            onSelect = { onSelect(if (it == 1) "wrappers" else "gpu") },
            focusable = true,
            fillWidth = false,
            height = 38.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 16.dp),
        )
    }
}

/** The wordmark: WinHub in DroidDeck's heavy style — "Win" light, "Hub" in the signal blue. */
@Composable
private fun DeckWordmark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Win",
            color = Deck.onBackground,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            maxLines = 1,
        )
        Text(
            text = "Hub",
            color = Deck.primary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Deck.primary),
        )
    }
}

/**
 * The left rail: every section on screen down the edge, 92dp wide, rounded 14 items that tint
 * with the signal blue when current — DroidDeck's SideRail, carrying this app's own sections.
 */
@Composable
private fun DeckSideRail(selected: String, focusRequester: FocusRequester, onSelect: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .background(Deck.surface)
            .padding(vertical = 12.dp),
    ) {
        deckSections().forEach { item ->
            DeckRailItem(
                label = item.label,
                icon = item.icon,
                current = selected == item.key,
                modifier = if (selected == item.key) Modifier.focusRequester(focusRequester) else Modifier,
                onClick = { onSelect(item.key) },
            )
        }
    }
}

@Composable
private fun DeckRailItem(
    label: String,
    icon: ImageVector,
    current: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // The selection tint glides between items instead of snapping — the rail "drags".
    val tint by animateColorAsState(
        targetValue = when {
            current -> Deck.primary.copy(alpha = 0.14f)
            focused -> Deck.surfaceVariant
            else -> Color.Transparent
        },
        animationSpec = tween(260, easing = DeckEase),
        label = "railTint",
    )
    val line by animateColorAsState(
        targetValue = if (current || focused) Deck.primary else Color.Transparent,
        animationSpec = tween(220, easing = DeckEase),
        label = "railLine",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(modifier)
            .padding(bottom = 4.dp)
            .size(width = 80.dp, height = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint)
            .border(
                width = 1.dp,
                color = line,
                shape = RoundedCornerShape(14.dp),
            )
            .deckBringIntoView()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (current) Deck.primary else Deck.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (current) Deck.primary else Deck.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (current) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(width = 3.dp, height = 26.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Deck.primary),
            )
        }
    }
}

/** The wall fades behind the words, and toward the bottom where the wordmark sits. */
@Composable
private fun DeckWallFade() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    0f to Deck.background.copy(alpha = 0.97f),
                    0.34f to Deck.background.copy(alpha = 0.86f),
                    0.68f to Deck.background.copy(alpha = 0.25f),
                    1f to Deck.background.copy(alpha = 0.1f),
                ),
            ),
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Deck.background.copy(alpha = 0.9f))),
    )
}

/**
 * Columns of capsules tilted a few degrees, alternate columns drifting up and down. Each column
 * holds its run of covers twice and moves by exactly one run, so the loop has no seam — the
 * geometry and timings are DroidDeck's CapsuleWall.
 */
@Composable
internal fun DeckCapsuleWall(covers: List<DeckCover>, driftMs: Int, modifier: Modifier = Modifier) {
    val capW = 112.dp
    val capH = 168.dp
    val gap = 14.dp
    val transition = rememberInfiniteTransition(label = "deckwall")
    val drift = transition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(driftMs, easing = LinearEasing)),
        label = "drift",
    )
    BoxWithConstraints(modifier.fillMaxSize()) {
        val tilt = Math.toRadians(13.0)
        val cosTilt = kotlin.math.cos(tilt).toFloat()
        val sinTilt = kotlin.math.sin(tilt).toFloat()
        val wallW = (maxWidth * cosTilt + maxHeight * sinTilt) * 1.1f
        val wallH = maxOf(maxHeight * 2.4f, (maxWidth * sinTilt + maxHeight * cosTilt) * 1.1f)
        val columns = ((wallW + gap) / (capW + gap)).toInt() + 1
        val perRun = ((wallH + gap) / (capH + gap)).toInt() + 1
        val run = (capH + gap) * perRun
        Row(
            horizontalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(x = -(wallW - maxWidth) / 2, y = -(wallH - maxHeight) / 2)
                .requiredSize(wallW, wallH)
                .graphicsLayer { rotationZ = -13f },
        ) {
            repeat(columns) { column ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier
                        .wrapContentHeight(Alignment.Top, unbounded = true)
                        .graphicsLayer {
                            val shift = run.toPx() * drift.value
                            translationY = if (column % 2 == 0) -shift else shift - run.toPx()
                        },
                ) {
                    repeat(perRun * 2) { index ->
                        val cover = covers[(column * 5 + (index % perRun) * 3) % covers.size]
                        DeckCapsule(cover, RoundedCornerShape(10.dp), Modifier.size(capW, capH))
                    }
                }
            }
        }
    }
}

/** One portrait capsule: the real cover art when bundled, otherwise a hue-tinted stand-in. */
@Composable
private fun DeckCapsule(cover: DeckCover, shape: RoundedCornerShape, modifier: Modifier) {
    if (cover.name.isEmpty()) {
        Box(
            modifier
                .clip(shape)
                .background(Brush.linearGradient(listOf(Deck.surface, Deck.background)))
                .border(1.dp, Deck.line, shape),
        )
        return
    }
    val bitmap = cover.bitmap
    if (bitmap != null) {
        Box(
            modifier
                .clip(shape)
                .background(Deck.surface)
                .border(1.dp, Deck.line.copy(alpha = 0.6f), shape),
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = cover.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        return
    }
    val hue = cover.hue
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.hsv(hue, 0.55f, 0.85f),
                        Color.hsv((hue + 0.12f) % 1f, 0.70f, 0.55f),
                    ),
                ),
            )
            .border(1.dp, Deck.line.copy(alpha = 0.6f), shape),
    ) {
        Text(
            text = cover.name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .alpha(0.92f),
        )
    }
}


/**
 * Console-style intro: the bundled `deck_intro.mp4` plays full-screen once when Deck Mode opens.
 * Tap to skip; auto-dismisses on completion or error.
 */
@Composable
private fun DeckIntro(onDone: () -> Unit) {
    val context = LocalContext.current
    var videoPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val out = File(context.cacheDir, "deck_intro.mp4")
            if (!out.exists()) {
                context.assets.open("deck_intro.mp4").use { input ->
                    FileOutputStream(out).use { output -> input.copyTo(output) }
                }
            }
            videoPath = out.absolutePath
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDone() },
        contentAlignment = Alignment.Center,
    ) {
        videoPath?.let { path ->
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setOnCompletionListener { onDone() }
                        setOnErrorListener { _, _, _ -> onDone(); true }
                        isClickable = false
                        setMediaController(null)
                    }
                },
                update = { view ->
                    if (view.tag != path) {
                        view.tag = path
                        view.setVideoPath(path)
                        view.start()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = "Tap to skip",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )
    }
}

/** Last played first, the rest in add order — the home's "Continue playing" shelf. */
private fun orderedForHome(list: List<HubEntry>, context: Context): List<HubEntry> {
    val last = context.getSharedPreferences("hub_prefs", Context.MODE_PRIVATE)
        .getString("last_played", null)
    val idx = list.indexOfFirst { it.game?.shortcutPath == last }
    return if (idx > 0) listOf(list[idx]) + list.filterIndexed { i, _ -> i != idx } else list
}

/** The big banner in the bannercode deck home: art on the right, title + facts on the left. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckHomeHero(
    entry: HubEntry,
    playtime: String,
    isContinue: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp)
            .clip(shape)
            .background(Deck.surface)
            .deckOuterFocusStroke(interaction, shape)
            .deckBringIntoView()
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
                onLongClick = onMenu,
            ),
    ) {
        entry.bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Deck.surface,
                        0.45f to Deck.surface.copy(alpha = 0.82f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Deck.background.copy(alpha = 0.9f),
                    ),
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Text(
                text = if (isContinue) "CONTINUE PLAYING" else "READY TO PLAY",
                color = Deck.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = entry.title,
                color = Color.White,
                fontSize = 26.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playtime.uppercase() + " PLAYED",
                color = Deck.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DeckButton("Play", primary = true, onClick = onPlay)
                DeckButton("Details", primary = false, onClick = onOpen)
            }
        }
    }
}

/**
 * One horizontal shelf with the Deck's dragging selection stroke: the ring stretches to the newly
 * focused card, then its trailing edge catches up — the same motion as the tabs, so the D-pad and
 * the stores row feel identical.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckHomeShelf(
    title: String,
    entries: List<HubEntry>,
    wide: Boolean,
    context: Context,
    onOpen: (HubEntry) -> Unit,
    onMenu: (HubEntry) -> Unit,
    onFocus: (HubEntry) -> Unit = {},
) {
    if (entries.isEmpty()) return
    val cardWidth = if (wide) 214.dp else 132.dp
    val cardHeight = if (wide) 120.dp else 198.dp
    val cardShape = RoundedCornerShape(16.dp)
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val lefts = remember(entries) { mutableStateListOf<Float>() }
    val rights = remember(entries) { mutableStateListOf<Float>() }
    var focused by remember(entries) { mutableIntStateOf(-1) }
    val start = remember { Animatable(0f) }
    val end = remember { Animatable(0f) }
    var lastFocused by remember { mutableIntStateOf(-1) }

    val targetLeft = lefts.getOrNull(focused)
    val targetRight = rights.getOrNull(focused)
    LaunchedEffect(focused, targetLeft, targetRight) {
        val l = targetLeft ?: return@LaunchedEffect
        val r = targetRight ?: return@LaunchedEffect
        when {
            lastFocused == -1 -> {
                start.snapTo(l)
                end.snapTo(r)
                lastFocused = focused
            }
            focused == lastFocused -> {
                if (start.value != l || end.value != r) {
                    start.snapTo(l)
                    end.snapTo(r)
                }
            }
            l >= start.value -> {
                coroutineScope {
                    launch { end.animateTo(r, tween(280, easing = DeckEase)) }
                    launch { start.animateTo(l, tween(240, delayMillis = 100, easing = DeckEase)) }
                }
                lastFocused = focused
            }
            else -> {
                coroutineScope {
                    launch { start.animateTo(l, tween(280, easing = DeckEase)) }
                    launch { end.animateTo(r, tween(240, delayMillis = 100, easing = DeckEase)) }
                }
                lastFocused = focused
            }
        }
    }
    // Keep the focused card (and the ring around it) inside the viewport.
    LaunchedEffect(focused) {
        val l = lefts.getOrNull(focused) ?: return@LaunchedEffect
        val edge = with(density) { 56.dp.toPx() }
        scroll.animateScrollTo((l - edge).coerceIn(0f, scroll.maxValue.toFloat()).roundToInt())
    }

    Column {
        Text(
            text = title.uppercase(),
            color = Deck.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .drawWithContent {
                    drawContent()
                    if (focused >= 0 && end.value > start.value) {
                        val gap = 3.dp.toPx()
                        val x = start.value - scroll.value - gap
                        drawRoundRect(
                            color = Deck.primary,
                            topLeft = Offset(x, -gap),
                            size = Size(
                                end.value - start.value + gap * 2f,
                                size.height + gap * 2f,
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx() + gap, 16.dp.toPx() + gap),
                            style = Stroke(width = 2.5.dp.toPx()),
                        )
                    }
                },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(scroll),
            ) {
                entries.forEachIndexed { index, entry ->
                    val interaction = remember(entry) { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                            .onGloballyPositioned { coords ->
                                val left = coords.positionInParent().x
                                while (lefts.size <= index) {
                                    lefts.add(0f)
                                    rights.add(0f)
                                }
                                if (lefts[index] != left) lefts[index] = left
                                val right = left + coords.size.width
                                if (rights[index] != right) rights[index] = right
                            }
                                            .clip(cardShape)
                            .background(Deck.surface)
                            // One stroke: the shelf ring sits outside the card, so the hairline
                            // border steps aside while this card is the focused one.
                            .border(
                                1.dp,
                                if (focused == index) Color.Transparent else Deck.line,
                                cardShape,
                            )
                            .deckBringIntoView()
                            .onFocusChanged {
                                if (it.isFocused) {
                                    focused = index
                                    onFocus(entry)
                                }
                            }
                            .combinedClickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onOpen(entry) },
                                onLongClick = { onMenu(entry) },
                            ),
                    ) {
                        entry.bitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = entry.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                            )
                        }
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0.55f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.78f),
                                    ),
                                ),
                        )
                        Text(
                            text = hubPlaytimeLabel(context, entry.title).uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(9.dp),
                        )
                    }
                }
            }
        }
    }
}
