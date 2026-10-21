package com.winlator.star.store

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.winlator.star.ui.screens.DeckStretchyTabs

/** One Deck tab: a label plus an optional live count badge. */
internal data class DeckTabSpec(val label: String, val badge: Int = 0)

/**
 * The storefront's Deck tab strip. The accent pill stretches from the old tab onto the new one
 * (DroidDeck's Steam-Deck-style tab feel) and the labels stay on the D-pad path, so STORE /
 * LIBRARY / SETTINGS can still be reached with the stick as well as the shoulders.
 */
@Composable
internal fun DeckTabBar(
    tabs: List<DeckTabSpec>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectOnFocus: Boolean = false,
) {
    DeckStretchyTabs(
        labels = tabs.map { it.label },
        badges = tabs.map { it.badge },
        selected = selected,
        onSelect = onSelect,
        focusable = true,
        selectOnFocus = selectOnFocus,
        modifier = modifier,
    )
}
