package com.winlator.star.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.star.store.AmazonLoginActivity
import com.winlator.star.store.AmazonMainActivity
import com.winlator.star.store.AmazonUserData
import com.winlator.star.store.DownloadManagerActivity
import com.winlator.star.store.EpicLoginActivity
import com.winlator.star.store.EpicMainActivity
import com.winlator.star.store.EpicUserData
import com.winlator.star.store.GogLoginActivity
import com.winlator.star.store.GogMainActivity
import com.winlator.star.store.GogUserData
import com.winlator.star.store.SteamLoginActivity
import com.winlator.star.store.SteamMainActivity
import com.winlator.star.store.SteamSessionManager
import com.winlator.star.ui.Screen
import com.winlator.star.ui.theme.AppThemeState

/**
 * Deck Mode's Stores section — a redesign of the store experience in DroidDeck's language, backed
 * by WinHub's REAL accounts (no mock data):
 *
 *  • The chip row is DroidDeck's `StoreChips` — a pill per store whose brand dot is lit only while
 *    that store's account is actually signed in — plus the shared Downloads chip.
 *  • Not signed in → DroidDeck's `SignInCard` hero: the store's tilted brand wall behind gradient
 *    scrims and a radial glow, its name at 42sp, and a large **Sign in** that opens WinHub's real
 *    login Activity for that store.
 *  • Signed in → the store's real cached account (name, and GOG's owned-game count) with Open /
 *    Downloads, opening the same store Activity the normal UI's drawer does.
 *
 * Sign-in is read live from each store's own session (`GogUserData`/`EpicUserData`/`AmazonUserData`
 * cached profiles and `SteamSessionManager`), so it stays truthful after a login round-trip.
 * Brand colours are DroidDeck's `sourceColours()` values.
 */
private data class DeckStoreTile(
    val screen: Screen,
    val chip: String,
    val fill: Color,
    val ink: Color,
    val dot: Color,
)

/** DroidDeck `sourceColours()` mapped onto WinHub's four store screens (order = `Screen.storeItems`). */
private val deckStores = listOf(
    DeckStoreTile(Screen.Gog, "GOG", Color(0xFF3A1A3C), Color(0xFFE6A3EA), Color(0xFFC25BC8)),
    DeckStoreTile(Screen.Epic, "Epic", Color(0xFF2A2A2A), Color(0xFFDDDDDD), Color(0xFFBDBDBD)),
    DeckStoreTile(Screen.Amazon, "Amazon", Color(0xFF3A2A0A), Color(0xFFFFC266), Color(0xFFFF9900)),
    DeckStoreTile(Screen.Steam, "Steam", Color(0xFF16293D), Color(0xFF7CC4FF), Color(0xFF1A9FFF)),
)

/** The store Activities `MainActivity.launchStore` starts — 1:1 with the normal UI drawer. */
private fun launchDeckStore(context: Context, screen: Screen) {
    val cls = when (screen) {
        Screen.Gog -> GogMainActivity::class.java
        Screen.Epic -> EpicMainActivity::class.java
        Screen.Amazon -> AmazonMainActivity::class.java
        Screen.Steam -> SteamMainActivity::class.java
        else -> return
    }
    context.startActivity(Intent(context, cls))
}

/** WinHub's real per-store login Activities (what each store's Sign in opens). */
private fun launchDeckLogin(context: Context, screen: Screen) {
    val cls = when (screen) {
        Screen.Gog -> GogLoginActivity::class.java
        Screen.Epic -> EpicLoginActivity::class.java
        Screen.Amazon -> AmazonLoginActivity::class.java
        Screen.Steam -> SteamLoginActivity::class.java
        else -> return
    }
    context.startActivity(Intent(context, cls))
}

/** Live sign-in, straight off each store's own session store. */
private fun deckSignedIn(context: Context, screen: Screen): Boolean = when (screen) {
    Screen.Gog -> GogUserData.cached(context) != null
    Screen.Epic -> EpicUserData.cached(context) != null
    Screen.Amazon -> AmazonUserData.cached(context) != null
    Screen.Steam -> SteamSessionManager.isLoggedOn()
    else -> false
}

/** The signed-in account's display name, when that store caches one. */
private fun deckAccountName(context: Context, screen: Screen): String? = when (screen) {
    Screen.Gog -> GogUserData.cached(context)?.username
    Screen.Epic -> EpicUserData.cached(context)?.displayName
    Screen.Amazon -> AmazonUserData.cached(context)?.name
    else -> null
}

/** GOG alone caches an owned-games count in its profile; the rest report nothing rather than guess. */
private fun deckOwnedCount(context: Context, screen: Screen): Int? =
    if (screen == Screen.Gog) GogUserData.cached(context)?.ownedGames else null

@Composable
internal fun DeckStoresScreen() {
    val context = LocalContext.current
    val showStores by AppThemeState.showStores.collectAsState()
    var selected by remember { mutableStateOf<Screen>(Screen.Gog) }
    val active = deckStores.firstOrNull { it.screen == selected } ?: deckStores.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Stores",
            color = Deck.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = "GOG, Epic Games, Amazon Games and Steam",
            color = Deck.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 2,
        )
        Spacer(Modifier.size(14.dp))

        if (!showStores) {
            DeckGroup("Stores are turned off") {
                DeckRow(
                    label = "Enable Stores",
                    hint = "Settings › Side Menu › Stores",
                    control = { DeckToggle(false) { } },
                )
            }
        } else {
            // ── Chip row: DroidDeck's StoreChips — the dot is lit only while signed in ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                deckStores.forEach { tile ->
                    val on = tile.screen == selected
                    val signed = deckSignedIn(context, tile.screen)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (on) tile.fill else Deck.surface)
                            .border(1.dp, if (on) tile.dot else Deck.line, RoundedCornerShape(99.dp))
                            .clickable { selected = tile.screen }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(tile.dot.copy(alpha = if (signed) 1f else 0.35f)),
                        )
                        Text(
                            text = tile.chip,
                            color = if (on) tile.ink else Deck.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // DroidDeck's fourth chip: the download queue the stores share.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Deck.surface)
                        .border(1.dp, Deck.line, RoundedCornerShape(99.dp))
                        .clickable { context.startActivity(Intent(context, DownloadManagerActivity::class.java)) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Filled.Download, contentDescription = null, tint = Deck.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Downloads",
                        color = Deck.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.size(16.dp))

            if (deckSignedIn(context, active.screen)) {
                DeckAccountCard(active, context)
            } else {
                DeckSignInCard(active, context)
            }
        }
    }
}

/**
 * DroidDeck's `SignInCard`: the store's tilted brand wall fading behind gradient scrims and a radial
 * glow, its name huge at the foot, and Sign in large beneath — here opening WinHub's real login.
 */
@Composable
private fun DeckSignInCard(tile: DeckStoreTile, context: Context) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(shape)
            .background(Deck.background)
            .border(1.dp, Deck.line, shape),
    ) {
        DeckTiltedWall(tile)
        // The words get the dark: side-lit from the right, weighted at the foot.
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Deck.background.copy(alpha = 0.97f),
                    0.45f to Deck.background.copy(alpha = 0.90f),
                    0.78f to Deck.background.copy(alpha = 0.35f),
                    1f to Deck.background.copy(alpha = 0.15f),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.5f to Color.Transparent, 1f to Deck.background.copy(alpha = 0.92f)),
            ),
        )
        // The store's own glow, low and to the left, under the words.
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(tile.dot.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.16f, size.height * 0.90f),
                        radius = size.height * 0.75f,
                    ),
                )
            },
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 40.dp, bottom = 36.dp, end = 20.dp),
        ) {
            Text(
                text = tile.screen.label,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black,
                color = Deck.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            DeckButton("Sign in", primary = true) { launchDeckLogin(context, tile.screen) }
        }
    }
}

/** The sign-in hero's background: the store's brand wall, tilted like DroidDeck's capsule wall. */
@Composable
private fun DeckTiltedWall(tile: DeckStoreTile) {
    val capsule = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = -13f; scaleX = 1.5f; scaleY = 1.5f },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            repeat(5) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                ) {
                    repeat(6) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(78.dp)
                                .clip(capsule)
                                .background(tile.fill)
                                .border(1.dp, tile.dot.copy(alpha = 0.22f), capsule),
                        )
                    }
                }
            }
        }
    }
}

/** Signed in: the store's real cached account, with the actions that open it — 1:1 with the drawer. */
@Composable
private fun DeckAccountCard(tile: DeckStoreTile, context: Context) {
    val name = deckAccountName(context, tile.screen)
    val owned = deckOwnedCount(context, tile.screen)
    DeckGroup(tile.screen.label) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // DroidDeck's StoreLogo: a rounded tile in the brand fill with its short label.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tile.fill)
                    .border(1.dp, tile.dot.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            ) {
                Text(
                    text = tile.chip,
                    color = tile.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name ?: "Signed in",
                    style = MaterialTheme.typography.titleMedium,
                    color = Deck.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (owned != null) "$owned games owned"
                    else "${tile.screen.label} account connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Deck.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.size(14.dp))
        DeckButton("Open ${tile.screen.label}", primary = true) {
            launchDeckStore(context, tile.screen)
        }
        DeckButton("Downloads", primary = false) {
            context.startActivity(Intent(context, DownloadManagerActivity::class.java))
        }
    }
}
