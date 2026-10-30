package com.winhub.hernandez.ui.screens

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
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.math.abs
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import com.winhub.hernandez.ui.moveFocusSafely
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.winhub.hernandez.R
import com.winhub.hernandez.XServerDisplayActivity
import com.winhub.hernandez.container.Container
import com.winhub.hernandez.contents.ContentProfile
import com.winhub.hernandez.contents.ContentsManager
import com.winhub.hernandez.container.ContainerManager
import com.winhub.hernandez.container.Shortcut
import com.winhub.hernandez.core.GameIdentifier
import com.winhub.hernandez.store.SaveManagerScreen
import com.winhub.hernandez.store.SteamPrefs
import com.winhub.hernandez.store.deckTileFocus
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalInputModeManager
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
import com.winhub.hernandez.ui.ControllerFocusIndication
import com.winhub.hernandez.ui.SocialContent
import com.winhub.hernandez.ui.theme.AppThemeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Deck shell palette. Delegates to [DeckPalette] (which is backed by the live theme preset),
 * so the shell, its hosted screens and its popups all recolor together. [attention] stays a fixed
 * amber warning semantic in every theme.
 */
internal object Deck {
    val background get() = DeckPalette.background
    val surface get() = DeckPalette.surface
    val surfaceVariant get() = DeckPalette.surfaceVariant
    val line get() = DeckPalette.line
    val onBackground get() = DeckPalette.onBackground
    val onSurfaceVariant get() = DeckPalette.onSurfaceVariant
    val primary get() = DeckPalette.primary
    val onPrimary get() = DeckPalette.onPrimary
    val secondary get() = DeckPalette.secondary
    val onSecondary get() = DeckPalette.onSecondary
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
    // Android flips the whole app to touch mode on every screen tap. Compose's `clickable` is
    // non-touch focusable, so that silently drops every shelf card / list row out of the D-pad
    // focus tree until the next hardware key — the hero survives only because it forces
    // canFocus. Deck Mode is controller-first, so pin Compose's input mode to Keyboard: touch
    // still works as before, focus never disappears.
    val platformInputMode = LocalInputModeManager.current
    val deckInputMode = remember(platformInputMode) { DeckInputModeManager(platformInputMode) }
    var introDone by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf("home") }
    var graphicPane by remember { mutableStateOf("gpu") }
    var deckDetail by remember { mutableStateOf<Int?>(null) }
    // General tab style: "horizontal" (top strip, default) or "vertical" (left rail).
    val tabStyle by AppThemeState.generalTabStyle.collectAsState()
    val horizontalRail = tabStyle != "vertical"
    val railSections = remember { deckSections() }

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
    // Right from the Home rail item must land on the hero. The "+" Add button shares the rail's
    // Y band, so the geometric search picked it first and the banner looked unreachable.
    val homeHeroFocus = remember { FocusRequester() }
    val tabs = remember { DeckTabHost() }
    // Top-strip style: the strip is not focusable, so after a section switch park focus on the
    // page's first control (unless the page already took focus itself).
    val focusManager = LocalFocusManager.current
    var contentFocused by remember { mutableStateOf(false) }
    LaunchedEffect(section, horizontalRail) {
        if (!horizontalRail) return@LaunchedEffect
        delay(450)
        if (!contentFocused) focusManager.moveFocusSafely(FocusDirection.Next)
    }
    val backLayer = remember { DeckBackLayer() }
    val editTracker = remember { DeckEditTracker() }
    LaunchedEffect(section, deckDetail) { tabs.bind(0) }
    // Park on the rail ONCE when the shell opens. Keying this on `section` re-parked the rail on
    // every section change, including changes triggered from inside the content, which is what
    // yanked the D-pad back to the sidebar and made the player redo the walk.
    LaunchedEffect(introDone) {
        if (introDone) runCatching { railFocus.requestFocus() }
    }
    // Back from a detail re-parks focus on the rail item we came from. Opening a detail parks
    // focus on its own Back button (see DeckContainerDetailScreen), never on an invisible node.
    LaunchedEffect(deckDetail) {
        if (deckDetail == null && introDone) {
            // Wait one frame: the closing detail is still animating out and owns focus, so an
            // immediate request can be dropped and the D-pad is left with nowhere to go.
            withFrameNanos { }
            runCatching { railFocus.requestFocus() }
        }
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
            secondary = Deck.secondary,
            onSecondary = Deck.onSecondary,
            secondaryContainer = Deck.secondary.copy(alpha = 0.30f),
            onSecondaryContainer = Deck.onBackground,
            background = Deck.background,
            onBackground = Deck.onBackground,
            surface = Deck.surface,
            onSurface = Deck.onBackground,
            surfaceVariant = Deck.surfaceVariant,
            onSurfaceVariant = Deck.onSurfaceVariant,
            error = DeckPalette.error,
        ),
    ) {
        CompositionLocalProvider(
            LocalInputModeManager provides deckInputMode,
            LocalIndication provides remember(Deck.primary) { ControllerFocusIndication(Deck.primary) },
            LocalDeckTabs provides tabs,
            LocalDeckBack provides backLayer,
            LocalDeckEditTracker provides editTracker,
            LocalDeckMode provides true,
        ) {
    // Horizontal style puts the sections in a top strip (L1/R1 switch them); vertical keeps the
    // left rail. Both host the same pane content below/right of the nav.
    if (horizontalRail) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Deck.background)
                // Behind the bar too, so a see-through top bar shows the page glow.
                .drawBehind { drawDeckCornerGlow(Deck.primary) }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.ButtonB -> {
                            if (editTracker.editing) false
                            else if (backLayer.onBack?.invoke() == true) true
                            else if (tabs.onBack?.invoke() == true) true
                            else { goBack(); true }
                        }
                        // L1/R1 switch the top-strip sections.
                        Key.ButtonL1 -> { stepSection(railSections, section, -1) { section = it; deckDetail = null }; true }
                        Key.ButtonR1 -> { stepSection(railSections, section, 1) { section = it; deckDetail = null }; true }
                        // L2/R2 switch the sub-tabs of the current page (Storage, editors, Contents).
                        Key.ButtonL2 -> if (tabs.count > 1) { tabs.step(-1); true } else false
                        Key.ButtonR2 -> if (tabs.count > 1) { tabs.step(1); true } else false
                        else -> false
                    }
                },
        ) {
            DeckTopTabs(
                sections = railSections,
                selected = section,
                onSelect = { next -> section = next; deckDetail = null },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onFocusChanged { contentFocused = it.hasFocus }
                    .clipToBounds(),
            ) {
                DeckPaneContent(
                    section = section,
                    deckDetail = deckDetail,
                    graphicPane = graphicPane,
                    onEditContainer = { id -> section = "containers"; deckDetail = id },
                    onDetailBack = { deckDetail = null },
                    onDetailOpen = { id -> deckDetail = id },
                    onGraphicPane = { graphicPane = it },
                    homeFocus = homeHeroFocus,
                )
                DeckLegend(Modifier.align(Alignment.BottomEnd))
                DeckCornerGlow()
            }
        }
    } else {
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
                    // Shoulders walk the active tab strip when the page has tabs bound (container
                    // detail, Save Manager, Contents), so switching a category no longer means
                    // walking focus all the way Up to the strip first. Pages without tabs keep the
                    // old wide horizontal focus step.
                    Key.ButtonL1 -> { stepSection(railSections, section, -1) { section = it; deckDetail = null }; true }
                    Key.ButtonR1 -> { stepSection(railSections, section, 1) { section = it; deckDetail = null }; true }
                    Key.ButtonL2 -> if (tabs.count > 1) { tabs.step(-1); true } else false
                    Key.ButtonR2 -> if (tabs.count > 1) { tabs.step(1); true } else false
                    else -> false
                }
            },
    ) {
        DeckSideRail(
            selected = section,
            focusRequester = railFocus,
            homeFocus = homeHeroFocus,
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
            DeckPaneContent(
                section = section,
                deckDetail = deckDetail,
                graphicPane = graphicPane,
                onEditContainer = { id -> section = "containers"; deckDetail = id },
                onDetailBack = { deckDetail = null },
                onDetailOpen = { id -> deckDetail = id },
                onGraphicPane = { graphicPane = it },
                homeFocus = homeHeroFocus,
            )
            DeckLegend(Modifier.align(Alignment.BottomEnd))
            DeckCornerGlow()
        }
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
private fun DeckHome(
    heroFocus: FocusRequester = remember { FocusRequester() },
    onEditContainer: (Int) -> Unit = {},
) {
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
    var launchMethodFor by remember { mutableStateOf<Shortcut?>(null) }
    var launchMethodSteam by remember { mutableStateOf<Boolean?>(null) }
    var addModeMenu by remember { mutableStateOf(false) }
    var addMode by remember { mutableStateOf(HubAddMode.MASTER) }
    var cronosBusy by remember { mutableStateOf(false) }
    var cronosName by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var propertiesFor by remember { mutableStateOf<Container?>(null) }
    var cronosReviewFor by remember { mutableStateOf<Container?>(null) }
    // A game added via Master Containers is a SHORTCUT inside a container you already have, so its
    // settings must edit that shortcut (per-game overrides), NOT the shared container. Set when the
    // game menu / detail page asks for Settings; the ContainerEditor is still reached from the
    // Storage section for whole-container changes.
    var shortcutSettingsFor by remember { mutableStateOf<Shortcut?>(null) }

    // Open a game's per-shortcut settings (a game added via Master Containers lives as a shortcut in
    // a shared container, so editing the container would change it for every game in it).
    fun openGameSettings(entry: HubEntry) {
        val g = entry.game ?: return
        val c = containers.find { it.id == g.containerId }
        when {
            c == null -> Toast.makeText(context, "Container not found", Toast.LENGTH_SHORT).show()
            !File(g.shortcutPath).isFile -> Toast.makeText(context, "Shortcut not found", Toast.LENGTH_SHORT).show()
            else -> shortcutSettingsFor = Shortcut(c, File(g.shortcutPath))
        }
    }

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
                // Read the container fresh from disk for THIS launch. `containers` is the list the
                // Home screen loaded when it opened; using it here launched with a stale config -
                // enabling LSFG in Settings and hitting Play still started the game with the old
                // "off" engine and the drawer said frame gen was off. ContainerManager's fresh
                // load also picks up a container created after Home was built.
                val container = ContainerManager(context).getContainerById(game.containerId)
                    ?: return@runCatching null
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

    fun openLaunchMethod(entry: HubEntry) {
        val game = entry.game
        if (game == null) {
            Toast.makeText(context, "Add this game first — use Add Games", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch(Dispatchers.IO) {
            val shortcut = runCatching {
                val container = ContainerManager(context).getContainerById(game.containerId)
                    ?: return@runCatching null
                val desktop = File(game.shortcutPath)
                if (!desktop.isFile) return@runCatching null
                Shortcut(container, desktop)
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (shortcut == null) {
                    Toast.makeText(context, "That game's shortcut is missing — re-add it", Toast.LENGTH_SHORT).show()
                } else {
                    launchMethodSteam = game.appId != null || game.store == "Steam"
                    launchMethodFor = shortcut
                }
            }
        }
    }

    fun addToHomeScreen(entry: HubEntry) {
        val g = entry.game ?: return
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
    }

    fun showProperties(entry: HubEntry) {
        val g = entry.game ?: return
        propertiesFor = containers.find { it.id == g.containerId }
        if (propertiesFor == null) Toast.makeText(context, "Container not found", Toast.LENGTH_SHORT).show()
    }

    fun deleteEntry(entry: HubEntry) {
        val g = entry.game ?: return
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
    }

    val backLayer = LocalDeckBack.current
    DisposableEffect(menuOpen) {
        backLayer.onBack = if (menuOpen) {
            { menuOpen = false; true }
        } else {
            null
        }
        onDispose { backLayer.onBack = null }
    }

    val wallCovers = remember(covers) {
        if (covers.isEmpty()) List(12) { index -> DeckCover("", index / 12f) } else covers
    }
    val current = entries?.getOrNull(selected)
    val actions = current?.let { entry ->
        listOf(
            DeckMenuAction("Play", Icons.Filled.PlayArrow) { playEntry(entry) },
            DeckMenuAction("Launch method", Icons.Filled.Layers) { openLaunchMethod(entry) },
            DeckMenuAction("Settings", Icons.Filled.Settings) { openGameSettings(entry) },
            DeckMenuAction("Add to Home Screen", Icons.Filled.Home) { addToHomeScreen(entry) },
            DeckMenuAction("Properties", Icons.Filled.Info) { showProperties(entry) },
            DeckMenuAction("Delete", Icons.Filled.Delete, danger = true) { deleteEntry(entry) },
        )
    }.orEmpty()

    // Hand focus back to the carousel once any dialog/overlay opened from Home goes away.
    val overlayOpen = pending != null || launchRequest != null || launchMethodFor != null ||
        propertiesFor != null || cronosReviewFor != null || shortcutSettingsFor != null || addModeMenu
    var hadOverlay by remember { mutableStateOf(false) }
    LaunchedEffect(overlayOpen) {
        if (!overlayOpen && hadOverlay) {
            delay(60)
            runCatching { heroFocus.requestFocus() }
        }
        hadOverlay = overlayOpen
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .clipToBounds(),
    ) {
        DeckCapsuleWall(
            covers = wallCovers,
            driftMs = if (covers.isEmpty()) 75_000 else 40_000,
        )
        DeckWallFade()

        DeckHomeCarousel(
            entries = entries.orEmpty(),
            selected = selected,
            onSelect = { selected = it },
            menuOpen = menuOpen,
            onMenuOpen = { menuOpen = it },
            actions = actions,
            focus = heroFocus,
        )
        if (entries == null) {
            CircularProgressIndicator(
                color = DeckPalette.primary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (entries.orEmpty().isEmpty()) {
            Text(
                "No games yet — tap +",
                color = DeckPalette.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Add Games: a glass + in the corner.
        val addInteraction = remember { MutableInteractionSource() }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 18.dp)
                .size(46.dp)
                .clip(CircleShape)
                .deckGlass(CircleShape)
                .deckFocusBorder(addInteraction, CircleShape, base = Color.Transparent)
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
            DeckGlassAddMenu(
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

        propertiesFor?.let { c ->
            DeckPropertiesDialog(container = c, onDismiss = { propertiesFor = null })
        }

        launchMethodFor?.let { s ->
            DeckLaunchMethodDialog(
                shortcut = s,
                onDismiss = { launchMethodFor = null; launchMethodSteam = null },
                steamOverride = launchMethodSteam,
                heading = "Launch method",
                confirmLabel = "Save",
                onLaunch = { mode, gm, remember ->
                    s.putExtra("launchMode", mode)
                    s.putExtra("launchModeRemembered", if (remember) "1" else "0")
                    if (gm != null) {
                        SteamPrefs.init(context)
                        SteamPrefs.setGoldbergMode(steamAppIdOf(s), gm)
                    }
                    s.saveData()
                    launchMethodFor = null
                    launchMethodSteam = null
                    Toast.makeText(context, "Launch method saved", Toast.LENGTH_SHORT).show()
                },
            )
        }

        shortcutSettingsFor?.let { sc ->
            DeckShortcutSettingsScreen(
                shortcut = sc,
                onDismiss = {
                    shortcutSettingsFor = null
                    // The editor can rename the game / swap its cover, so re-read the store to keep
                    // the carousel tile in sync.
                    scope.launch(Dispatchers.IO) {
                        val refreshed = HubGamesStore.load(context).map { g ->
                            HubEntry(g.name, g.coverPath?.let { BitmapFactory.decodeFile(it) }, g)
                        }
                        withContext(Dispatchers.Main) { entries = refreshed }
                    }
                },
            )
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
            selectOnFocus = true,
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

/** Move the section selection forward/back, wrapping at the ends. */
private fun stepSection(
    sections: List<DeckSection>,
    current: String,
    delta: Int,
    onSelect: (String) -> Unit,
) {
    if (sections.isEmpty()) return
    val idx = sections.indexOfFirst { it.key == current }.coerceAtLeast(0)
    val next = ((idx + delta) % sections.size + sections.size) % sections.size
    onSelect(sections[next].key)
}

/** The pane body shared by both rail styles: section switch + the per-section screens. */
@Composable
private fun DeckPaneContent(
    section: String,
    deckDetail: Int?,
    graphicPane: String,
    onEditContainer: (Int) -> Unit,
    onDetailBack: () -> Unit,
    onDetailOpen: (Int) -> Unit,
    onGraphicPane: (String) -> Unit,
    homeFocus: FocusRequester,
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
            "home" -> DeckHome(heroFocus = homeFocus, onEditContainer = onEditContainer)
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
                    // Per-id key: the screen's remembered sections/focus must not survive a
                    // switch from one container (or New / Defaults) to another.
                    key(detailId) {
                        DeckContainerDetailScreen(
                            containerId = detailId,
                            onNavigateBack = onDetailBack,
                        )
                    }
                } else {
                    DeckContainersScreen(onEditContainer = onDetailOpen)
                }
            }
            "stores" -> DeckStoresScreen()
            "file_manager" -> FileManagerScreen(deckMode = true)
            "graphics" -> DeckGraphicsPane(
                pane = graphicPane,
                onSelect = onGraphicPane,
            )
            "input_controls" -> InputControlsScreen()
            "contents" -> DeckContentsScreen()
            // The full Save Manager (Steam / Custom / Epic / GOG / Settings), in Deck chrome.
            "saves" -> SaveManagerScreen(deckMode = true)
            "social_hub" -> SocialContent()
            "settings" -> DeckSettingsScreen()
            // Defensive fallback only; never composes alongside the "home" branch, so it
            // gets its own requester instead of sharing the rail's homeHeroFocus.
            else -> DeckHome(onEditContainer = onEditContainer)
        }
    }
}

/** Controller legend: the one place a player can look to know what L1/R1 do here.
 *  Draw-only text, so it never becomes a focus stop. */
@Composable
private fun DeckLegend(modifier: Modifier = Modifier) {
    Text(
        text = "A Select   B Back   L1/R1 Tabs   L2/R2 Sub-tabs   Stick / D-pad Move",
        color = Deck.onSurfaceVariant.copy(alpha = 0.75f),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = modifier.padding(end = 14.dp, bottom = 8.dp),
    )
}

/** DroidDeck's corner light — one circular accent glow just inside the top right of every pane.
 *  Draw-only, so it never intercepts touches meant for the screen underneath. */
@Composable
private fun DeckCornerGlow() {
    val cornerAccent = Deck.primary
    Box(modifier = Modifier.fillMaxSize().drawBehind { drawDeckCornerGlow(cornerAccent) })
}

private fun DrawScope.drawDeckCornerGlow(cornerAccent: Color) {
    val radius = 460.dp.toPx()
    val center = Offset(size.width - radius * 0.35f, radius * 0.35f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                cornerAccent.copy(alpha = 0.24f),
                cornerAccent.copy(alpha = 0.10f),
                cornerAccent.copy(alpha = 0.04f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * The horizontal top strip: every section's blue icon across the top. The selected one shows its
 * name to the right of the icon; unselected ones are just the icon. L1/R1 (handled by the shell)
 * switch sections, so this strip itself is not a focus target.
 */
@Composable
private fun DeckTopTabs(
    sections: List<DeckSection>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val barOpacity by AppThemeState.topBarOpacity.collectAsState()
    // The selection pill glides between tabs (its trailing edge leads, then the leading edge
    // catches up) — the same "dragged" motion as the side rail. The strip is NOT a focus target:
    // L1/R1 switch sections, so D-pad focus stays in the page instead of bouncing up to the bar.
    val lefts = remember { mutableStateMapOf<String, Float>() }
    val rights = remember { mutableStateMapOf<String, Float>() }
    val start = remember { Animatable(0f) }
    val end = remember { Animatable(0f) }
    val selLeft = lefts[selected]
    val selRight = rights[selected]
    LaunchedEffect(selected, selLeft, selRight) {
        val l = selLeft ?: return@LaunchedEffect
        val r = selRight ?: return@LaunchedEffect
        if (end.value <= start.value) {
            start.snapTo(l); end.snapTo(r)
            return@LaunchedEffect
        }
        coroutineScope {
            if (l >= start.value) {
                launch { end.animateTo(r, tween(280, easing = DeckEase)) }
                launch { start.animateTo(l, tween(240, delayMillis = 100, easing = DeckEase)) }
            } else {
                launch { start.animateTo(l, tween(280, easing = DeckEase)) }
                launch { end.animateTo(r, tween(240, delayMillis = 100, easing = DeckEase)) }
            }
        }
    }
    val accent = Deck.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Deck.surface.copy(alpha = barOpacity))
            .drawBehind {
                if (end.value > start.value) {
                    val top = 6.dp.toPx()
                    val corner = CornerRadius(12.dp.toPx())
                    val pill = Size(end.value - start.value, size.height - top * 2f)
                    drawRoundRect(accent.copy(alpha = 0.16f), Offset(start.value, top), pill, corner)
                    drawRoundRect(accent, Offset(start.value, top), pill, corner, style = Stroke(2.dp.toPx()))
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            sections.forEach { item ->
                val current = selected == item.key
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .onGloballyPositioned {
                            val x = it.positionInParent().x
                            lefts[item.key] = x
                            rights[item.key] = x + it.size.width
                        }
                        .focusProperties { canFocus = false }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onSelect(item.key)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = Deck.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    if (current) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            color = Deck.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(color = Deck.line.copy(alpha = barOpacity))
}

/**
 * The left rail: every section on screen down the edge, 92dp wide, rounded 14 items that tint
 * with the signal blue when current — DroidDeck's SideRail, carrying this app's own sections.
 */
@Composable
private fun DeckSideRail(
    selected: String,
    focusRequester: FocusRequester,
    homeFocus: FocusRequester,
    onSelect: (String) -> Unit,
) {
    DeckDragFrame(
        modifier = Modifier
            .width(92.dp)
            .fillMaxHeight()
            .background(Deck.surface),
        shape = RoundedCornerShape(14.dp),
        strokeWidth = 2.5.dp,
        gap = 2.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
        ) {
            deckSections().forEach { item ->
                DeckRailItem(
                    label = item.label,
                    icon = item.icon,
                    current = selected == item.key,
                    modifier = if (selected == item.key) Modifier.focusRequester(focusRequester) else Modifier,
                    // Only Home has a competing focus target (the "+" button) in its beam.
                    rightFocus = if (item.key == "home") homeFocus else null,
                    onFocus = { if (selected != item.key) onSelect(item.key) },
                    onClick = { onSelect(item.key) },
                )
            }
        }
    }
}

@Composable
private fun DeckRailItem(
    label: String,
    icon: ImageVector,
    current: Boolean,
    modifier: Modifier = Modifier,
    rightFocus: FocusRequester? = null,
    onFocus: () -> Unit,
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
                color = if (focused) Color.Transparent else line,
                shape = RoundedCornerShape(14.dp),
            )
            .deckBringIntoView()
            .deckDragItem("rail:$label")
            .focusProperties { if (rightFocus != null) right = rightFocus }
            .onFocusChanged { if (it.isFocused) onFocus() }
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

/** One XMB row of the game's quick menu. */
private class DeckMenuAction(
    val label: String,
    val icon: ImageVector,
    val danger: Boolean = false,
    val run: () -> Unit,
)

/** Frosted-glass surface: a light vertical sheen, a soft specular glow and a bright rim. */
private fun Modifier.deckGlass(shape: Shape, strength: Float = 1f): Modifier = this
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.20f * strength), Color.White.copy(alpha = 0.06f * strength)),
        ),
        shape,
    )
    .background(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.18f * strength), Color.Transparent),
            center = Offset(80f, 0f),
            radius = 420f,
        ),
        shape,
    )
    .border(
        1.dp,
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.22f),
            ),
        ),
        shape,
    )

/**
 * Home carousel: covers glide along a row anchored top-left, the selected one grows and gets the
 * accent stroke. D-pad Down folds the row up and opens the XMB quick menu under the cover.
 * Left/Right browse, A plays, Up/B close the menu. The whole pane is ONE focus target so the
 * controller never fights per-tile focus; touch taps and swipes drive the same state.
 */
@Composable
private fun DeckHomeCarousel(
    entries: List<HubEntry>,
    selected: Int,
    onSelect: (Int) -> Unit,
    menuOpen: Boolean,
    onMenuOpen: (Boolean) -> Unit,
    actions: List<DeckMenuAction>,
    focus: FocusRequester,
) {
    val context = LocalContext.current
    val accent = Deck.primary
    val pos = remember { Animatable(selected.toFloat()) }
    val menuT = remember { Animatable(0f) }
    val menuPos = remember { Animatable(0f) }
    var menuIdx by remember { mutableIntStateOf(0) }
    var menuVisible by remember { mutableStateOf(false) }
    val selNow by rememberUpdatedState(selected)
    val countNow by rememberUpdatedState(entries.size)
    val menuNow by rememberUpdatedState(menuOpen)

    LaunchedEffect(selected) { pos.animateTo(selected.toFloat(), tween(300, easing = DeckEase)) }
    LaunchedEffect(menuOpen) {
        if (menuOpen) {
            menuIdx = 0
            menuPos.snapTo(0f)
            menuVisible = true
            menuT.animateTo(1f, tween(220, easing = DeckEase))
        } else {
            menuT.animateTo(0f, tween(180, easing = DeckEase))
            menuVisible = false
        }
    }
    LaunchedEffect(menuIdx) { menuPos.animateTo(menuIdx.toFloat(), tween(180, easing = DeckEase)) }

    val n = entries.size
    val current = entries.getOrNull(selected)
    val coverShape = RoundedCornerShape(14.dp)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val top = 76.dp
        val x0 = 28.dp
        // Room for the grown cover (x1.35) plus the title line under it.
        val baseH = ((maxHeight - top - 72.dp) / 1.35f).coerceIn(100.dp, 180.dp)
        val baseW = baseH * 2f / 3f
        val rowH = 40.dp

        Box(
            Modifier
                .fillMaxSize()
                .focusProperties { canFocus = true }
                .focusRequester(focus)
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown || countNow == 0) return@onPreviewKeyEvent false
                    val fresh = e.nativeKeyEvent.repeatCount == 0
                    if (menuOpen) {
                        when (e.key) {
                            Key.DirectionUp -> { if (menuIdx > 0) menuIdx-- else onMenuOpen(false); true }
                            Key.DirectionDown -> { if (menuIdx < actions.lastIndex) menuIdx++; true }
                            Key.DirectionLeft, Key.DirectionRight -> true
                            Key.ButtonA, Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                                if (fresh) {
                                    val action = actions.getOrNull(menuIdx)
                                    onMenuOpen(false)
                                    action?.run?.invoke()
                                }
                                true
                            }
                            Key.Escape -> { onMenuOpen(false); true }
                            else -> false
                        }
                    } else {
                        when (e.key) {
                            Key.DirectionLeft -> if (selected > 0) { onSelect(selected - 1); true } else false
                            Key.DirectionRight -> { if (selected < n - 1) onSelect(selected + 1); true }
                            Key.DirectionDown -> { onMenuOpen(true); true }
                            Key.ButtonA, Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> {
                                if (fresh) actions.firstOrNull()?.run?.invoke()
                                true
                            }
                            else -> false
                        }
                    }
                }
                .focusable()
                .pointerInput(Unit) {
                    var acc = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { acc = 0f },
                        onDragCancel = { acc = 0f },
                    ) { change, dx ->
                        if (menuNow) return@detectHorizontalDragGestures
                        change.consume()
                        acc += dx
                        val threshold = 56.dp.toPx()
                        if (acc <= -threshold && selNow < countNow - 1) {
                            onSelect(selNow + 1)
                            acc = 0f
                        } else if (acc >= threshold && selNow > 0) {
                            onSelect(selNow - 1)
                            acc = 0f
                        }
                    }
                }
                .pointerInput(Unit) { detectTapGestures { if (menuNow) onMenuOpen(false) } },
        ) {
            val lo = (selected - 8).coerceAtLeast(0)
            val hi = (selected + 8).coerceAtMost(n - 1)
            for (i in lo..hi) {
                val entry = entries[i]
                key(i) {
                    Box(
                        Modifier
                            .size(baseW, baseH)
                            .graphicsLayer {
                                val t = menuT.value
                                val m = 1f - 0.5f * t
                                val bw = baseW.toPx() * m
                                val step = bw + 14.dp.toPx() * m
                                val d = i - pos.value
                                val grow = (1f - abs(d)).coerceIn(0f, 1f)
                                val s = m * (1f + 0.35f * grow)
                                translationX = x0.toPx() + step * d + 0.35f * bw * d.coerceIn(0f, 1f)
                                translationY = top.toPx()
                                scaleX = s
                                scaleY = s
                                transformOrigin = TransformOrigin(0f, 0f)
                                // Left of the selection fades out; behind an open menu the rest dims.
                                alpha = (1f + d).coerceIn(0f, 1f) * (1f - 0.7f * t * (1f - grow))
                            }
                            .clip(coverShape)
                            .background(Deck.surface)
                            .drawWithContent {
                                drawContent()
                                val w = (1f - abs(i - pos.value)).coerceIn(0f, 1f)
                                if (w > 0.02f) {
                                    val sw = 3.dp.toPx()
                                    drawRoundRect(
                                        color = accent.copy(alpha = w),
                                        topLeft = Offset(sw / 2f, sw / 2f),
                                        size = Size(size.width - sw, size.height - sw),
                                        cornerRadius = CornerRadius(14.dp.toPx()),
                                        style = Stroke(width = sw),
                                    )
                                }
                            }
                            .pointerInput(i, selected, menuOpen) {
                                detectTapGestures {
                                    when {
                                        i != selected -> onSelect(i)
                                        else -> onMenuOpen(!menuOpen)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val bmp = entry.bitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = entry.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                            )
                        } else {
                            Text(
                                entry.title,
                                color = Deck.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }

            if (current != null) {
                val playtime = remember(current) { hubPlaytimeLabel(context, current.title) }
                Column(
                    Modifier
                        .offset {
                            val t = menuT.value
                            val selW = baseW.toPx() * 1.35f * (1f - 0.5f * t)
                            val closedY = top.toPx() + baseH.toPx() * 1.35f + 10.dp.toPx()
                            val openX = x0.toPx() + selW + 16.dp.toPx()
                            val openY = top.toPx() + 2.dp.toPx()
                            IntOffset(
                                (x0.toPx() + (openX - x0.toPx()) * t).roundToInt(),
                                (closedY + (openY - closedY) * t).roundToInt(),
                            )
                        }
                        .widthIn(max = 300.dp),
                ) {
                    Text(
                        current.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        hubOriginLabel(current).uppercase() + "  ·  " +
                            if (playtime == "not played yet") playtime else "$playtime played",
                        color = Deck.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                    )
                }
            }

            if (menuVisible) {
                val menuTop = top + baseH * 0.675f + 12.dp
                actions.forEachIndexed { idx, action ->
                    val focused = idx == menuIdx
                    val tint = if (action.danger) DeckPalette.error else Deck.onSurfaceVariant
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .graphicsLayer {
                                val rel = idx - menuPos.value
                                translationX = x0.toPx() - (1f - menuT.value) * 12.dp.toPx()
                                translationY = menuTop.toPx() + rel * rowH.toPx()
                                // Rows already passed fade out above the focused one, like the XMB.
                                alpha = menuT.value * (1f + rel).coerceIn(0f, 1f)
                            }
                            .height(rowH)
                            .clip(CircleShape)
                            .then(if (focused) Modifier.deckGlass(CircleShape) else Modifier)
                            .pointerInput(idx) {
                                detectTapGestures {
                                    menuIdx = idx
                                    onMenuOpen(false)
                                    action.run()
                                }
                            }
                            .padding(horizontal = 8.dp),
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (focused) accent else Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                action.icon,
                                contentDescription = null,
                                tint = if (focused && !action.danger) Deck.onPrimary else tint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            action.label,
                            color = if (action.danger) DeckPalette.error else if (focused) Color.White else Deck.onSurfaceVariant,
                            fontSize = if (focused) 16.sp else 14.sp,
                            fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(end = 10.dp),
                        )
                    }
                }
            }

            Text(
                text = if (menuOpen) "A  Select      B  Back" else "A  Play      ↓  Options      ◀ ▶  Browse",
                color = Deck.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 12.dp),
            )
        }
    }
}

/** The Add Games chooser in liquid glass: frosted panel (real backdrop blur on Android 12+). */
@Composable
private fun DeckGlassAddMenu(onPick: (HubAddMode) -> Unit, onDismiss: () -> Unit) {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = 40 }
        }
    }
    val panel = RoundedCornerShape(28.dp)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.8f)
                .widthIn(max = 520.dp)
                .clip(panel)
                .deckGlass(panel)
                .deckDialogNav(onDismiss)
                .padding(22.dp),
        ) {
            Text("Add games", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Where should it live?",
                color = Deck.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
            )
            DeckGlassChoice(
                "Master Containers",
                "Add it as a shortcut inside a container you already have.",
                Icons.Filled.Layers,
            ) { onPick(HubAddMode.MASTER) }
            Spacer(Modifier.height(10.dp))
            DeckGlassChoice(
                "Cronos",
                "A fresh dedicated container built just for this game.",
                Icons.Filled.SportsEsports,
            ) { onPick(HubAddMode.CRONOS) }
            Text(
                "B  Back",
                color = Deck.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun DeckGlassChoice(title: String, desc: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .deckGlass(shape, 0.8f)
            .deckFocusBorder(interaction, shape, base = Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(14.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Deck.primary.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Deck.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/**
 * Deck Mode reports [InputMode.Keyboard] no matter what Android's touch mode says, so Compose's
 * non-touch-focusable modifiers (`clickable`, `combinedClickable`, …) stay reachable with the
 * controller after the user touches the screen. Input-mode requests are still forwarded to the
 * real manager so keyboard/IME side effects behave normally.
 */
@OptIn(ExperimentalComposeUiApi::class)
private class DeckInputModeManager(
    private val delegate: InputModeManager,
) : InputModeManager {
    override val inputMode: InputMode get() = InputMode.Keyboard

    override fun requestInputMode(inputMode: InputMode): Boolean =
        delegate.requestInputMode(inputMode)
}
