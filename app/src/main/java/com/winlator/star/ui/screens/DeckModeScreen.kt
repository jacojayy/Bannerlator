package com.winlator.star.ui.screens

import android.app.Activity

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.winlator.star.container.ContainerManager
import com.winlator.star.ui.ControllerFocusIndication
import com.winlator.star.ui.SocialContent
import com.winlator.star.ui.screens.contents.ContentsHubScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The DroidDeck palette (Graphite), lifted verbatim from DroidDeck's ui/Theme.kt. */
private object Deck {
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

/** One tile on the wall: a name (drives its colour) and the resolved hue. */
internal class DeckCover(val name: String, val hue: Float)

private fun hueOf(name: String): Float {
    var h = 0
    for (c in name) h = 31 * h + c.code
    return ((h % 360) + 360) % 360 / 360f
}

/** The rail's real sections — the app's own destinations, in the DroidDeck rail style. */
private class DeckSection(val key: String, val label: String, val icon: ImageVector)

private fun deckSections() = listOf(
    DeckSection("home", "Home", Icons.Filled.Apps),
    DeckSection("containers", "Containers", Icons.Filled.Folder),
    DeckSection("file_manager", "Files", Icons.Filled.FolderOpen),
    DeckSection("graphics", "Graphics", Icons.Filled.Memory),
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
    var titles by remember { mutableStateOf<List<String>>(emptyList()) }
    var section by remember { mutableStateOf("home") }
    var graphicPane by remember { mutableStateOf("gpu") }
    var deckDetail by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val manager = ContainerManager(context)
            manager.reloadContainers()
            titles = manager.getContainers().map { it.name }
        }
    }

    // Truly full screen: the system status/nav bars go with the top bar, restored on leaving.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val controller = (context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
        if (controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
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
        CompositionLocalProvider(LocalIndication provides remember { ControllerFocusIndication(Deck.primary) }) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background),
    ) {
        DeckSideRail(
            selected = section,
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
            when (section) {
                "home" -> DeckHome(titles = titles)
                "containers" -> {
                    val detailId = deckDetail
                    if (detailId != null) {
                        ContainerDetailScreen(
                            containerId = detailId,
                            onNavigateBack = { deckDetail = null },
                        )
                    } else {
                        ContainersScreen(
                            onNavigateToDetail = { id -> deckDetail = id ?: -1 },
                            onOpenDeck = { },
                        )
                    }
                }
                "file_manager" -> FileManagerScreen()
                "graphics" -> DeckGraphicsPane(
                    pane = graphicPane,
                    onSelect = { graphicPane = it },
                )
                "input_controls" -> InputControlsScreen()
                "contents" -> ContentsHubScreen()
                "saves" -> SavesScreen()
                "social_hub" -> SocialContent()
                "settings" -> SettingsScreen(onSaved = { })
                else -> DeckHome(titles = titles)
            }

            // Leave Deck Mode — the only chrome that sits over a hosted screen.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 16.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Deck.surface)
                    .border(1.dp, Deck.line, RoundedCornerShape(14.dp))
                    .clickable { onExit() },
            ) {
                Text(
                    text = "✕",
                    color = Deck.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
        }
    }
}

/** Home: the drifting capsule wall, the wordmark, and nothing that pretends to launch. */
@Composable
private fun DeckHome(titles: List<String>) {
    val covers = remember(titles) {
        if (titles.isEmpty()) List(12) { index -> DeckCover("", index / 12f) }
        else titles.map { title -> DeckCover(title, hueOf(title)) }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .clipToBounds(),
    ) {
        DeckCapsuleWall(
            covers = covers,
            driftMs = if (titles.isEmpty()) 75_000 else 40_000,
        )
        DeckWallFade()
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 36.dp, bottom = 44.dp, end = 20.dp),
        ) {
            DeckWordmark()
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 12.dp, start = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Deck.surface)
                .border(1.dp, Deck.line, RoundedCornerShape(14.dp))
                .padding(6.dp),
        ) {
            DeckPaneChip(
                label = "GPU drivers",
                selected = pane == "gpu",
                onClick = { onSelect("gpu") },
            )
            DeckPaneChip(
                label = "Wrappers",
                selected = pane == "wrappers",
                onClick = { onSelect("wrappers") },
            )
        }
    }
}

@Composable
private fun DeckPaneChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Deck.primary.copy(alpha = 0.14f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) Deck.primary else Color.Transparent,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Deck.primary else Deck.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

/** The wordmark: BANNERLATOR in DroidDeck's heavy, tight-tracked style with the signal dot. */
@Composable
private fun DeckWordmark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "BANNERLATOR",
            color = Deck.onBackground,
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
private fun DeckSideRail(selected: String, onSelect: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .fillMaxHeight()
            .background(Deck.surface)
            .padding(vertical = 12.dp),
    ) {
        deckSections().forEach { item ->
            DeckRailItem(
                label = item.label,
                icon = item.icon,
                current = selected == item.key,
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
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .size(width = 80.dp, height = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (current) Deck.primary.copy(alpha = 0.14f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (current) Deck.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() },
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
private fun DeckCapsuleWall(covers: List<DeckCover>, driftMs: Int, modifier: Modifier = Modifier) {
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

/** One portrait capsule: a hue-tinted gradient standing in for the cover art. */
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
