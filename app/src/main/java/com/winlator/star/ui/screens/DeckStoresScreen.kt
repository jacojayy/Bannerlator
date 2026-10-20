package com.winlator.star.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import com.winlator.star.store.GogGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.star.store.AmazonLoginActivity
import com.winlator.star.store.CatalogItem
import com.winlator.star.store.DeckRise
import com.winlator.star.store.DeckTabBar
import com.winlator.star.store.DeckTabSpec
import com.winlator.star.store.EpicStoreCatalog
import com.winlator.star.store.GogStoreCatalog
import com.winlator.star.store.SteamStoreCatalog
import com.winlator.star.store.StoreCatalogDetailActivity
import com.winlator.star.store.deckTileFocus
import com.winlator.star.store.AmazonUserData
import com.winlator.star.store.AmazonGame
import com.winlator.star.store.AmazonGameDetailActivity
import com.winlator.star.store.AmazonLibraryRepo
import com.winlator.star.store.EpicGame
import com.winlator.star.store.EpicGameDetailActivity
import com.winlator.star.store.EpicLibraryRepo
import com.winlator.star.store.GogGameDetailActivity
import com.winlator.star.store.GogLibraryRepo
import com.winlator.star.store.SteamGame
import com.winlator.star.store.SteamGameDetailActivity
import com.winlator.star.store.SteamRepository
import com.winlator.star.store.DeckStoreTheme
import com.winlator.star.store.SteamFriendsStore
import com.winlator.star.store.SteamFriendsTab
import com.winlator.star.store.SteamPrefs
import com.winlator.star.store.SteamProfileTab
import com.winlator.star.store.StoreMatureSetting
import com.winlator.star.store.StoreDownloadTier
import com.winlator.star.store.rememberSteamLibrary
import com.winlator.star.store.rememberStorefrontLayout
import com.winlator.star.store.download.DownloadRegistry
import android.widget.Toast
import com.winlator.star.store.EpicLoginActivity
import com.winlator.star.store.EpicUserData
import com.winlator.star.store.GogLoginActivity
import com.winlator.star.store.GogUserData
import com.winlator.star.store.SteamLoginActivity
import com.winlator.star.store.SteamSessionManager
import com.winlator.star.ui.Screen
import com.winlator.star.ui.theme.AppThemeState

/**
 * Deck Mode's Stores section — a redesign of the store experience in DroidDeck's language, backed
 * by WinHub's REAL accounts (no mock data):
 *
 *  • The store row is a Deck stretchy tab strip — one tab per store, brand dot lit only while that
 *    store's account is actually signed in — with the shared Downloads tile in the title row.
 *  • Not signed in → DroidDeck's `SignInCard` hero: the store's tilted brand wall behind gradient
 *    scrims and a radial glow, its name at 42sp, and a large **Sign in** that opens WinHub's real
 *    login Activity for that store.
 *  • Signed in → storefront, one unified Library grid across every signed-in store (games only),
 *    Settings, and for Steam the Friends/Profile panes.
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

/** One owned game: cover art, and how to open its real per-store detail screen. */
private data class DeckOwnedGame(
    val title: String,
    val art: String?,
    val open: (Context) -> Unit,
    val gog: GogGame? = null,
    val epic: EpicGame? = null,
    val amazon: AmazonGame? = null,
    val steamAppId: Int = 0,
)

/**
 * The store's REAL cached owned-games library, read off each store's own repo. Steam's rows come off
 * `SteamRepository`'s SQLite cache (its header art needs no hash). Wrapped in `runCatching` so a
 * store that has never synced simply draws nothing rather than taking the screen down.
 */
private fun deckOwnedGames(context: Context, screen: Screen): List<DeckOwnedGame> = runCatching {
    when (screen) {
        Screen.Gog -> GogLibraryRepo.cached(context).map { g ->
            val c = GogLibraryRepo.toCatalogItem(g)
            DeckOwnedGame(
                title = g.title,
                art = c.tallImageUrl ?: c.imageUrl,
                gog = g,
                open = { ctx ->
                    ctx.startActivity(Intent(ctx, GogGameDetailActivity::class.java).apply {
                        putExtra("game_id", g.gameId)
                        putExtra("title", g.title)
                        putExtra("image_url", g.imageUrl)
                        putExtra("description", g.description)
                        putExtra("developer", g.developer)
                        putExtra("category", g.category)
                        putExtra("generation", g.generation)
                    })
                },
            )
        }
        Screen.Epic -> EpicLibraryRepo.cached(context).map { g ->
            DeckOwnedGame(
                title = g.title,
                art = g.artCover.ifBlank { null } ?: g.artSquare.ifBlank { null },
                epic = g,
                open = { ctx ->
                    ctx.startActivity(Intent(ctx, EpicGameDetailActivity::class.java).apply {
                        putExtra("app_name", g.appName)
                        putExtra("title", g.title)
                        putExtra("description", g.description)
                        putExtra("developer", g.developer)
                        putExtra("art_cover", g.artCover)
                        putExtra("namespace", g.namespace)
                        putExtra("catalog_item_id", g.catalogItemId)
                    })
                },
            )
        }
        Screen.Amazon -> AmazonLibraryRepo.cached(context).map { g ->
            DeckOwnedGame(
                title = g.title,
                art = g.artUrl.ifBlank { null } ?: g.heroUrl.ifBlank { null },
                amazon = g,
                open = { ctx ->
                    ctx.startActivity(Intent(ctx, AmazonGameDetailActivity::class.java).apply {
                        putExtra("product_id", g.productId)
                        putExtra("entitlement_id", g.entitlementId)
                        putExtra("title", g.title)
                        putExtra("developer", g.developer)
                        putExtra("publisher", g.publisher)
                        putExtra("art_url", g.artUrl)
                        putExtra("product_sku", g.productSku)
                    })
                },
            )
        }
        Screen.Steam -> SteamRepository.getInstance().getCachedGameRows()
            .map { SteamGame.fromGameRow(it) }
            .map { g ->
                DeckOwnedGame(
                    title = g.name,
                    art = g.headerUrl,
                    steamAppId = g.appId,
                    open = { ctx ->
                        ctx.startActivity(
                            Intent(ctx, SteamGameDetailActivity::class.java)
                                .putExtra(SteamGameDetailActivity.EXTRA_APP_ID, g.appId),
                        )
                    },
                )
            }
        else -> emptyList()
    }
}.getOrDefault(emptyList())

/** The selected store's own cached library, sorted by title — never mixed with other stores. */
private fun deckLibraryGames(context: Context, screen: Screen): List<DeckOwnedGame> = runCatching {
    deckOwnedGames(context, screen).sortedBy { it.title.lowercase() }
}.getOrDefault(emptyList())

/** One store's library: its own full-height cover grid, reloaded when the store tab changes. */
@Composable
private fun DeckLibraryPane(screen: Screen, context: Context, onGameClick: (DeckOwnedGame) -> Unit) {
    var games by remember(screen) { mutableStateOf<List<DeckOwnedGame>?>(null) }
    LaunchedEffect(screen) {
        games = withContext(Dispatchers.IO) { deckLibraryGames(context, screen) }
    }
    val list = games
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "LIBRARY",
                color = Deck.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
            )
            if (list != null && list.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${list.size} game${if (list.size == 1) "" else "s"}",
                    color = Deck.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            list == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading library...", color = Deck.onSurfaceVariant, fontSize = 13.sp)
            }
            list.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No games yet. Sign in to this store and sync to fill its library.",
                    color = Deck.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(list, key = { index, game -> "$index-${game.title}" }) { index, game ->
                    DeckRise(index = index % 10) {
                        DeckGameCard(game) { onGameClick(game) }
                    }
                }
            }
        }
    }
}

/** A library block: the cover fills the tile, a gradient fade grounds the title at its foot. */
@Composable
private fun DeckGameCard(game: DeckOwnedGame, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .deckTileFocus(interaction)
            .clip(shape)
            .background(Deck.surface)
            .border(1.dp, Deck.line, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        val art = game.art
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Deck.background.copy(alpha = 0.92f),
                    ),
                ),
        )
        Text(
            text = game.title,
            color = Deck.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        )
    }
}

/** A browseable storefront entry: cover art, title, price and how to open its detail page. */
private data class DeckBrowseItem(
    val title: String,
    val art: String?,
    val price: String,
    val catalog: CatalogItem? = null,
    val appId: Int = 0,
    val open: (Context) -> Unit,
)

/** A labeled rail of storefront items, DroidDeck style. */
private data class DeckBrowseSection(val title: String, val items: List<DeckBrowseItem>)

/** A store's featured catalog: a hero pick plus labeled rails. */
private data class DeckStoreData(val hero: DeckBrowseItem?, val sections: List<DeckBrowseSection>)

private fun deckCatalogPrice(i: CatalogItem): String = when {
    i.isFree -> "Free"
    i.hasPrice && i.finalPrice.isNotBlank() -> i.finalPrice
    else -> ""
}

private fun deckCatalogBrowse(i: CatalogItem): DeckBrowseItem =
    DeckBrowseItem(
        title = i.title,
        art = i.tallImageUrl ?: i.imageUrl,
        price = deckCatalogPrice(i),
        open = { ctx -> ctx.startActivity(StoreCatalogDetailActivity.intent(ctx, i)) },
        catalog = i,
    )

private fun deckSteamBrowse(i: SteamStoreCatalog.StoreItem): DeckBrowseItem =
    DeckBrowseItem(
        title = i.name,
        art = i.artUrl,
        price = when {
            i.isFree -> "Free"
            i.hasPrice -> SteamStoreCatalog.formatPrice(i.finalCents, i.currency)
            else -> ""
        },
        appId = i.appId,
    ) { ctx ->
        ctx.startActivity(
            Intent(ctx, SteamGameDetailActivity::class.java)
                .putExtra(SteamGameDetailActivity.EXTRA_APP_ID, i.appId),
        )
    }

private fun <T> List<T>.rail(title: String, toItem: (T) -> DeckBrowseItem): DeckBrowseSection? =
    if (isEmpty()) null else DeckBrowseSection(title, map(toItem))

/** The store's REAL featured catalog, fetched off the main thread. Amazon ships no browse catalog. */
private suspend fun deckFeatured(context: Context, screen: Screen): DeckStoreData = when (screen) {
    Screen.Gog -> runCatching { GogStoreCatalog.featured(context) }.getOrNull().let { f ->
        if (f == null) DeckStoreData(null, emptyList())
        else DeckStoreData(
            hero = f.hero?.let { deckCatalogBrowse(it) },
            sections = listOfNotNull(
                f.trending.rail("Trending") { deckCatalogBrowse(it) },
                f.newReleases.rail("New Releases") { deckCatalogBrowse(it) },
                f.deals.rail("Deals") { deckCatalogBrowse(it) },
                f.free.rail("Free") { deckCatalogBrowse(it) },
            ),
        )
    }
    Screen.Epic -> runCatching { EpicStoreCatalog.featured(context) }.getOrNull().let { f ->
        if (f == null) DeckStoreData(null, emptyList())
        else DeckStoreData(
            hero = f.hero?.let { deckCatalogBrowse(it) },
            sections = listOfNotNull(
                f.freeNow.rail("Free Now") { deckCatalogBrowse(it) },
                f.onSale.rail("On Sale") { deckCatalogBrowse(it) },
                f.newReleases.rail("New Releases") { deckCatalogBrowse(it) },
                f.freeToPlay.rail("Free to Play") { deckCatalogBrowse(it) },
            ),
        )
    }
    Screen.Steam -> runCatching { SteamStoreCatalog.featured(context) }.getOrNull().let { f ->
        if (f == null) DeckStoreData(null, emptyList())
        else DeckStoreData(
            hero = f.hero?.let { deckSteamBrowse(it) },
            sections = listOfNotNull(
                f.newReleases.rail("New Releases") { deckSteamBrowse(it) },
                f.topFree.rail("Top Free") { deckSteamBrowse(it) },
                f.specials.rail("Specials") { deckSteamBrowse(it) },
            ),
        )
    }
    else -> DeckStoreData(null, emptyList())
}

/** The signed-in store's storefront: a hero pick plus labeled rails of real featured titles. */
@Composable
private fun DeckStoreFront(
    tile: DeckStoreTile,
    context: Context,
    onCard: (DeckBrowseItem) -> Unit,
    showMature: Boolean,
) {
    var data by remember(tile.screen) { mutableStateOf<DeckStoreData?>(null) }
    LaunchedEffect(tile.screen) {
        data = withContext(Dispatchers.IO) { deckFeatured(context, tile.screen) }
    }
    val raw = data
    if (raw == null) {
        Text("Loading ${tile.screen.label} store...", color = Deck.onSurfaceVariant, fontSize = 12.sp)
        return
    }
    // Mature titles stay out of the hero and rails unless the store setting shows them; Steam
    // items carry no catalog, so they always pass (`catalog?.mature != true`).
    val store = if (showMature) raw else DeckStoreData(
        hero = raw.hero?.takeIf { it.catalog?.mature != true },
        sections = raw.sections.mapNotNull { section ->
            val items = section.items.filter { it.catalog?.mature != true }
            if (items.isEmpty()) null else DeckBrowseSection(section.title, items)
        },
    )
    if (store.hero == null && store.sections.isEmpty()) {
        Text(
            "No featured titles cached yet - open ${tile.screen.label} to refresh.",
            color = Deck.onSurfaceVariant,
            fontSize = 12.sp,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        store.hero?.let { hero -> DeckHeroCard(hero) { onCard(hero) } }
        store.sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = section.title,
                    color = Deck.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(section.items, key = { i, it -> "$i-${it.title}" }) { index, item ->
                        DeckRise(index = index % 8) { DeckBrowseCard(item) { onCard(item) } }
                    }
                }
            }
        }
    }
}

/** The hero pick: a big cover banner with the title and price over a graphite tile. */
@Composable
private fun DeckHeroCard(item: DeckBrowseItem, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .deckTileFocus(interaction)
            .clip(shape)
            .background(Deck.surface)
            .border(1.dp, Deck.line, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        val art = item.art
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Deck.background.copy(alpha = 0.92f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                color = Deck.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.price.isNotEmpty()) {
                Text(
                    text = item.price,
                    color = if (item.price == "Free") Deck.primary else Deck.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** A storefront rail card: cover art over graphite, title and price beneath. */
@Composable
private fun DeckBrowseCard(item: DeckBrowseItem, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .width(150.dp)
            .deckTileFocus(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(shape)
                .background(Deck.surface)
                .border(1.dp, Deck.line, shape),
        ) {
            val art = item.art
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Spacer(Modifier.size(6.dp))
        Text(
            text = item.title,
            color = Deck.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.price.isNotEmpty()) {
            Text(
                text = item.price,
                color = if (item.price == "Free") Deck.primary else Deck.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun DeckStoresScreen() {
    val context = LocalContext.current
    val showStores by AppThemeState.showStores.collectAsState()
    var selected by remember { mutableStateOf<Screen>(Screen.Gog) }
    var gogInstall by remember { mutableStateOf<GogGame?>(null) }
    var epicInstall by remember { mutableStateOf<EpicGame?>(null) }
    var amazonInstall by remember { mutableStateOf<AmazonGame?>(null) }
    var steamInstall by remember { mutableStateOf<DeckSteamTarget?>(null) }
    var showDownloads by remember { mutableStateOf(false) }
    var showMature by remember { mutableStateOf(StoreMatureSetting.get(context)) }
    val dlEntries by DownloadRegistry.entries.collectAsState()
    val activeDownloads = dlEntries.count { it.isActive }
    LaunchedEffect(Unit) { DownloadRegistry.init(context) }
    val active = deckStores.firstOrNull { it.screen == selected } ?: deckStores.first()
    val scope = rememberCoroutineScope()

    // Library (owned) card tap -> in-Deck install when the store's payload is attached.
    val openOwned: (DeckOwnedGame) -> Unit = { g ->
        when {
            g.gog != null -> gogInstall = g.gog
            g.epic != null -> epicInstall = g.epic
            g.amazon != null -> amazonInstall = g.amazon
            g.steamAppId != 0 -> steamInstall = DeckSteamTarget(g.steamAppId, g.title, g.art)
            else -> g.open(context)
        }
    }

    // Store-tab browse card tap -> in-Deck install (GOG resolves its gen flag; Epic matches the
    // owned library for appName/namespace; Steam goes straight from appId). Unowned fallbacks
    // keep the legacy detail page.
    val onBrowseTap: (DeckBrowseItem) -> Unit = { item ->
        val cat = item.catalog
        when {
            active.screen == Screen.Gog && cat != null -> scope.launch {
                val game = withContext(Dispatchers.IO) {
                    deckGogBuild(
                        context, cat.id, cat.title, cat.imageUrl,
                        cat.description, cat.developer, cat.tags,
                    )
                }
                gogInstall = game
            }
            active.screen == Screen.Epic && cat != null -> scope.launch {
                val owned = withContext(Dispatchers.IO) {
                    EpicLibraryRepo.cached(context).firstOrNull { g ->
                        (g.namespace == cat.extra["namespace"] && g.catalogItemId == cat.id) ||
                            g.title.equals(cat.title, ignoreCase = true)
                    }
                }
                if (owned != null) epicInstall = owned else item.open(context)
            }
            active.screen == Screen.Steam && item.appId != 0 ->
                steamInstall = DeckSteamTarget(item.appId, item.title, item.art)
            else -> item.open(context)
        }
    }

    when {
        showDownloads -> DeckDownloadsPanel {
            // Pull the panel's Store-settings change back so the storefront refilters at once.
            showMature = StoreMatureSetting.get(context)
            showDownloads = false
        }
        gogInstall != null -> {
            val g = gogInstall!!
            DeckGogInstall(
                game = g,
                onBack = { gogInstall = null },
                onOpenStore = {
                    context.startActivity(Intent(context, GogGameDetailActivity::class.java).apply {
                        putExtra("game_id", g.gameId)
                        putExtra("title", g.title)
                        putExtra("image_url", g.imageUrl)
                        putExtra("description", g.description)
                        putExtra("developer", g.developer)
                        putExtra("category", g.category)
                        putExtra("generation", g.generation)
                    })
                },
            )
        }
        epicInstall != null -> {
            val g = epicInstall!!
            DeckEpicInstall(
                game = g,
                onBack = { epicInstall = null },
                onOpenStore = {
                    context.startActivity(Intent(context, EpicGameDetailActivity::class.java).apply {
                        putExtra("app_name", g.appName)
                        putExtra("title", g.title)
                        putExtra("description", g.description)
                        putExtra("developer", g.developer)
                        putExtra("art_cover", g.artCover)
                        putExtra("namespace", g.namespace)
                        putExtra("catalog_item_id", g.catalogItemId)
                    })
                },
            )
        }
        amazonInstall != null -> {
            val g = amazonInstall!!
            DeckAmazonInstall(
                game = g,
                onBack = { amazonInstall = null },
                onOpenStore = {
                    context.startActivity(Intent(context, AmazonGameDetailActivity::class.java).apply {
                        putExtra("product_id", g.productId)
                        putExtra("entitlement_id", g.entitlementId)
                        putExtra("title", g.title)
                        putExtra("developer", g.developer)
                        putExtra("publisher", g.publisher)
                        putExtra("art_url", g.artUrl)
                        putExtra("product_sku", g.productSku)
                    })
                },
            )
        }
        steamInstall != null -> {
            val t = steamInstall!!
            DeckSteamInstall(
                target = t,
                onBack = { steamInstall = null },
                onOpenStore = {
                    context.startActivity(
                        Intent(context, SteamGameDetailActivity::class.java)
                            .putExtra(SteamGameDetailActivity.EXTRA_APP_ID, t.appId),
                    )
                },
            )
        }
        else -> {
    // NOT globally scrollable: the tab panes below (Store settings, Steam's Profile) carry their
    // OWN verticalScroll, and a scrollable measured with infinite height throws
    // "Vertically scrollable component was measured with an infinity maximum height constraints".
    // Chrome (title, chips, tab strip) stays fixed; the pane gets the bounded remainder.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            }
            // Shared download queue: compact graphite tile with a live count.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Deck.surfaceVariant)
                    .border(1.dp, Deck.line, RoundedCornerShape(13.dp))
                    .clickable { showDownloads = true }
                    .padding(horizontal = 12.dp),
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = "Downloads",
                    tint = if (activeDownloads > 0) Deck.primary else Deck.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
                if (activeDownloads > 0) {
                    Text(
                        text = activeDownloads.toString(),
                        color = Deck.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.size(14.dp))

        if (!showStores) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                DeckPaneScroll {
                    DeckGroup("Stores are turned off") {
                        DeckRow(
                            label = "Enable Stores",
                            hint = "Settings › Side Menu › Stores",
                            control = { DeckToggle(false) { } },
                        )
                    }
                }
            }
        } else {
            // ── Store tabs: the Deck stretchy strip (stroke expands then closes), brand dot lit
            // only while signed in. Downloads lives in the title row above.
            DeckStretchyTabs(
                labels = deckStores.map { it.chip },
                dots = deckStores.map { tile ->
                    tile.dot.copy(alpha = if (deckSignedIn(context, tile.screen)) 1f else 0.35f)
                },
                selected = deckStores.indexOfFirst { it.screen == selected }.coerceAtLeast(0),
                onSelect = { selected = deckStores[it].screen },
                scroll = true,
                height = 38.dp,
            )

            Spacer(Modifier.size(16.dp))

            if (deckSignedIn(context, active.screen)) {
                var storeTab by remember(active.screen) { mutableStateOf(0) }
                // Steam alone carries the social pair: Friends + Profile, the same composables
                // the store front embeds, in the Deck tab strip. Every store gets Settings.
                val tabs = if (active.screen == Screen.Steam) {
                    listOf(
                        DeckTabSpec("Store"),
                        DeckTabSpec("Library"),
                        DeckTabSpec("Settings"),
                        DeckTabSpec("Friends"),
                        DeckTabSpec("Profile"),
                    )
                } else {
                    listOf(
                        DeckTabSpec("Store"),
                        DeckTabSpec("Library"),
                        DeckTabSpec("Settings"),
                    )
                }
                DeckTabBar(
                    tabs = tabs,
                    selected = storeTab,
                    onSelect = { storeTab = it },
                )
                Spacer(Modifier.size(12.dp))
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    when (storeTab) {
                        0 -> DeckPaneScroll {
                            DeckStoreFront(active, context, onBrowseTap, showMature)
                        }
                        1 -> DeckLibraryPane(active.screen, context, openOwned)
                        2 -> DeckStoreSettingsPane(
                            showMature = showMature,
                            onShowMatureChange = { v ->
                                showMature = v
                                StoreMatureSetting.set(context, v)
                            },
                            onOpenDownloads = { showDownloads = true },
                            activeDownloads = activeDownloads,
                        )
                        3 -> DeckSteamFriendsPane()
                        else -> DeckSteamProfilePane(onOpenFriends = { storeTab = 3 })
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    DeckPaneScroll { DeckSignInCard(active, context) }
                }
            }
        }
    }
    }
    }
}

/**
 * One Store tab pane: a bounded, self-scrolling host. The Store chrome (title, chips, tab strip) is
 * fixed, so the pane sits in a `weight(1f)` Box with FINITE height — which is exactly what the
 * panes carrying their own `verticalScroll` (Store settings, Steam's Profile) need. A scrollable
 * measured inside an infinite-height constraint throws
 * "Vertically scrollable component was measured with an infinity maximum height constraints".
 */
@Composable
private fun DeckPaneScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        content = content,
    )
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

/**
 * The Settings tab: the shared store settings (mature-content filter + download-speed tier — the
 * same ones the Downloads pane carries, so both entry points work) plus a jump into the queue.
 * [showMature] is hoisted by [DeckStoresScreen] so the storefront refilters the instant it flips.
 */
@Composable
private fun DeckStoreSettingsPane(
    showMature: Boolean,
    onShowMatureChange: (Boolean) -> Unit,
    onOpenDownloads: () -> Unit,
    activeDownloads: Int,
) {
    val context = LocalContext.current
    var dlTier by remember { mutableStateOf(StoreDownloadTier.get(context)) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DeckGroup("Store settings") {
            DeckRow(
                label = "Show mature content",
                hint = "Storefront 17+/18+ titles; owned games always show",
                control = { DeckToggle(showMature, onShowMatureChange) },
            )
            DeckChoiceRow(
                label = "Download speed",
                hint = "Ceiling the next store download may ramp to",
                options = StoreDownloadTier.ALL.map { t -> t.toString() to StoreDownloadTier.label(t) },
                selected = dlTier.toString(),
                onPick = { v ->
                    v.toIntOrNull()?.let { t ->
                        dlTier = t
                        StoreDownloadTier.set(context, t)
                    }
                },
            )
        }
        DeckGroup("Queue") {
            DeckRow(
                label = "Downloads",
                hint = if (activeDownloads > 0) "$activeDownloads active" else "Nothing downloading",
                control = { DeckButton("Open", primary = false) { onOpenDownloads() } },
            )
        }
    }
}

/**
 * Steam's Friends tab embedded in the Deck tab strip: the SAME composable the store front runs,
 * wrapped in [DeckStoreTheme] so it draws in graphite, with the session inits the store Activity
 * would otherwise have done. Messages surface as a Toast (the Deck shell has no snackbar host).
 */
@Composable
private fun DeckSteamFriendsPane() {
    val context = LocalContext.current
    // SteamMainActivity runs these in onCreate — composition-time, before ANY child reads a
    // pref or the engine, never deferred to an effect.
    remember(context) {
        SteamPrefs.init(context)
        SteamRepository.getInstance().initialize(context)
        SteamFriendsStore.init(context)
        true
    }
    val layout = rememberStorefrontLayout()
    DeckStoreTheme {
        SteamFriendsTab(
            wide = layout.wide,
            onMessage = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
        )
    }
}

/**
 * Steam's Profile tab in the Deck tab strip — self-loading like the store front's, so the library
 * count comes from the sanctioned [rememberSteamLibrary] host and Open Friends flips the tab strip.
 */
@Composable
private fun DeckSteamProfilePane(onOpenFriends: () -> Unit) {
    val context = LocalContext.current
    // Same composition-time session init as the Friends pane — see there for why it must not
    // wait for an effect: everything the profile tab touches expects SteamMainActivity's onCreate
    // sequence to have already run.
    remember(context) {
        SteamPrefs.init(context)
        SteamRepository.getInstance().initialize(context)
        SteamFriendsStore.init(context)
        true
    }
    val library = rememberSteamLibrary()
    val layout = rememberStorefrontLayout()
    DeckStoreTheme {
        SteamProfileTab(
            wide = layout.wide,
            libraryCount = library.games.size,
            onOpenFriends = onOpenFriends,
            onOpenApp = { appId ->
                context.startActivity(
                    Intent(context, SteamGameDetailActivity::class.java)
                        .putExtra(SteamGameDetailActivity.EXTRA_APP_ID, appId),
                )
            },
            onMessage = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
        )
    }
}
