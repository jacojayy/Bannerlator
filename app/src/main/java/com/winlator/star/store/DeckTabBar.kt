package com.winlator.star.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.star.ui.screens.Deck

/** One Deck tab: a label plus an optional live count badge. */
internal data class DeckTabSpec(val label: String, val badge: Int = 0)

/**
 * A Deck-styled, controller-first tab strip that replaces Material's [androidx.compose.material3.TabRow]
 * on the storefront. Pill tabs scale on focus (DroidDeck's tile motion), the selected tab fills with
 * the Deck accent, and a focus ring marks the D-pad cursor. LEFT/RIGHT traverses and center/Enter
 * selects — both come from [clickable], which Compose wires to the DPAD-CENTER and ENTER keys.
 */
@Composable
internal fun DeckTabBar(
    tabs: List<DeckTabSpec>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Deck.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEachIndexed { index, spec ->
            DeckTabItem(
                spec = spec,
                selected = index == selected,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DeckTabItem(
    spec: DeckTabSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (focused) 1.04f else 1f,
        animationSpec = DeckMotion.sp(0.55f, androidx.compose.animation.core.Spring.StiffnessMedium),
        label = "deckTabScale",
    )
    val bg = when {
        selected -> Deck.primary
        focused -> Deck.surfaceVariant
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bg)
            .border(1.dp, if (focused && !selected) Deck.primary else Color.Transparent, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = spec.label,
            color = if (selected) Deck.onPrimary else Deck.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
        )
        if (spec.badge > 0) {
            Text(
                text = " (${spec.badge})",
                color = if (selected) Deck.onPrimary else Deck.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}
