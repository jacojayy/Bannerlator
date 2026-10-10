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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.star.store.AmazonMainActivity
import com.winlator.star.store.DownloadManagerActivity
import com.winlator.star.store.EpicMainActivity
import com.winlator.star.store.GogMainActivity
import com.winlator.star.store.SteamMainActivity
import com.winlator.star.ui.Screen
import com.winlator.star.ui.theme.AppThemeState

/**
 * Deck Mode's Stores section — a visual clone of DroidDeck's Stores page (a pill chip row of store
 * brands over a selected-store pane, in the shared graphite palette) driven by WinHub's REAL stores.
 *
 * Functionality is 1:1 with the normal UI's Stores section (see `AppDrawer.DrawerStoreItem` +
 * `MainActivity.launchStore`): every action opens the very same store Activity the app drawer does —
 * GOG/Epic/Amazon/Steam — plus `DownloadManagerActivity` for the queue. The section also honours the
 * same `AppThemeState.showStores` gate the drawer uses, so turning Stores off hides it here too.
 *
 * Brand colours are DroidDeck's own `sourceColours()` values, so the chips read as their stores.
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

/** The same store Activities `MainActivity.launchStore` starts — 1:1 with the normal UI. */
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

@Composable
internal fun DeckStoresScreen() {
    val context = LocalContext.current
    val showStores by AppThemeState.showStores.collectAsState()
    var selected by remember { mutableStateOf(Screen.Gog) }
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
            text = "GOG, Epic Games, Amazon Games and Steam — same stores as the app drawer",
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
            // ── Chip row: DroidDeck's StoreChip, one pill per store + the downloads chip ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                deckStores.forEach { tile ->
                    val on = tile.screen == selected
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
                                .background(tile.dot),
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
                // DroidDeck's fourth chip: the shared download queue for all three (here four) stores.
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
                    Icon(Icons.Filled.Download, contentDescription = null, tint = Deck.onSurfaceVariant,
                        modifier = Modifier.size(16.dp))
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

            // ── Selected-store pane: DroidDeck's StoreLogo tile + the action that opens it ──
            DeckGroup(active.screen.label) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // StoreLogo clone: rounded tile in the brand fill with its short label.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(active.fill)
                            .border(1.dp, active.dot.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    ) {
                        Text(
                            text = active.chip,
                            color = active.ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = active.screen.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = Deck.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Opens the ${active.screen.label} store app, exactly as the drawer does.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Deck.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.size(14.dp))
                DeckButton("Open ${active.screen.label}", primary = true) {
                    launchDeckStore(context, active.screen)
                }
                DeckButton("Downloads", primary = false) {
                    context.startActivity(Intent(context, DownloadManagerActivity::class.java))
                }
            }

            Spacer(Modifier.size(14.dp))

            // ── Every store, as the app drawer lists them (1:1 rows → same Activities) ──
            DeckGroup("All Stores") {
                deckStores.forEach { tile ->
                    DeckRow(
                        label = tile.screen.label,
                        control = {
                            DeckButton("Open", primary = true) {
                                launchDeckStore(context, tile.screen)
                            }
                        },
                    )
                }
            }
        }
    }
}
