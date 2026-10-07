package com.winlator.star.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The DroidDeck palette (Graphite), lifted verbatim from DroidDeck's ui/Theme.kt. */
private object Deck {
    val background = Color(0xFF0A0B0D)
    val surface = Color(0xFF121417)
    val surfaceVariant = Color(0xFF1A1D22)
    val line = Color(0xFF262A31)
    val onBackground = Color(0xFFF2F4F7)
    val onSurfaceVariant = Color(0xFF9AA3AF)
    val primary = Color(0xFF1A9FFF)
    val primary2 = Color(0xFF1487DB)
    val onPrimary = Color(0xFF03111F)
    val good = Color(0xFF4CD37F)
    val attention = Color(0xFFFFB547)
}

/** One tile on the wall: a name (drives its colour) and the resolved hue. */
internal class DeckCover(val name: String, val hue: Float)

private fun hueOf(name: String): Float {
    var h = 0
    for (c in name) h = 31 * h + c.code
    return ((h % 360) + 360) % 360 / 360f
}

/**
 * Deck Mode — DroidDeck's front end, ported as UI only: the drifting, tilted capsule wall behind
 * the wordmark, the left rail and the one thing to do here, Play. Nothing in it launches anything.
 */
@Composable
internal fun DeckModeScreen(titles: List<String>, onExit: () -> Unit) {
    val covers = remember(titles) {
        if (titles.isEmpty()) List(12) { index -> DeckCover("", index / 12f) }
        else titles.map { title -> DeckCover(title, hueOf(title)) }
    }
    var selected by remember { mutableStateOf("steam") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .clipToBounds(),
    ) {
        if (titles.isEmpty()) {
            DeckCapsuleWall(covers = covers, driftMs = 75_000)
        } else {
            DeckCapsuleWall(covers = covers, driftMs = 40_000)
            DeckWallFade()
        }

        DeckSideRail(
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.align(Alignment.CenterStart),
        )

        // Top-right: leave Deck Mode.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 20.dp)
                .size(48.dp)
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

        // Bottom-left: wordmark, then Play and the cog — DroidDeck's SteamHome block.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 136.dp, bottom = 44.dp, end = 20.dp),
        ) {
            DeckWordmark()
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeckPrimaryButton(label = "Play", onClick = { })
                DeckCog(size = 54.dp, onClick = { })
            }
        }
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

/** The primary action: a signal-blue pill, white-play glyph in the DroidDeck on-signal colour. */
@Composable
private fun DeckPrimaryButton(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Deck.primary)
            .clickable { onClick() }
            .padding(horizontal = 26.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Deck.onPrimary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            color = Deck.onPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** The cog beside Play: a large round hit target in DroidDeck's 54dp size. */
@Composable
private fun DeckCog(size: Dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Deck.surfaceVariant)
            .border(1.dp, Deck.line, CircleShape)
            .clickable { onClick() },
    ) {
        Text(text = "⚙", color = Deck.onBackground, fontSize = 24.sp)
    }
}

/**
 * The left rail: every section on screen down the edge, 92dp wide, rounded 14 items that tint
 * with the signal blue when current — DroidDeck's SideRail.
 */
@Composable
private fun DeckSideRail(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(92.dp)
            .fillMaxHeight()
            .background(Deck.surface)
            .padding(vertical = 12.dp),
    ) {
        val items = listOf(
            Triple("steam", "Steam", Icons.Outlined.SportsEsports),
            Triple("games", "Games", Icons.Outlined.VideoLibrary),
            Triple("desktop", "Desktop", Icons.Outlined.DesktopWindows),
            Triple("store", "Store", Icons.Outlined.Storefront),
            Triple("components", "Components", Icons.Outlined.Layers),
            Triple("setup", "Setup", Icons.Outlined.Tune),
            Triple("updates", "Updates", Icons.Outlined.SystemUpdate),
        )
        items.forEach { (key, label, icon) ->
            DeckRailItem(
                label = label,
                icon = icon,
                current = selected == key,
                badge = key == "setup" || key == "updates",
                onClick = { onSelect(key) },
            )
        }
        Spacer(Modifier.weight(1f))
        // The running session, always one press away (DroidDeck's ResumeRailItem).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .width(80.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Deck.good.copy(alpha = 0.12f))
                .clickable { }
                .padding(vertical = 9.dp, horizontal = 4.dp),
        ) {
            Box(
                modifier = Modifier.size(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Deck.good),
                )
            }
            Text(
                text = "Resume",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Deck.onBackground,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = "Session",
                fontSize = 12.sp,
                color = Deck.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DeckRailItem(
    label: String,
    icon: ImageVector,
    current: Boolean,
    badge: Boolean,
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
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 18.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Deck.attention),
            )
        }
    }
}

/** The wall fades behind the words, and toward the bottom where the buttons sit. */
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
