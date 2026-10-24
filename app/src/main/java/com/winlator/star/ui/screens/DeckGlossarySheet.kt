package com.winlator.star.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.star.ui.components.CONTAINER_GLOSSARY

/**
 * Deck-native glossary popup replacing the shared Material [com.winlator.star.ui.components.ContainerGlossarySheet]
 * for the Deck UI. Reuses the same [CONTAINER_GLOSSARY] data and search/filter semantics.
 */
@Composable
internal fun DeckGlossarySheet(onDismiss: () -> Unit, initialQuery: String = "") {
    var query by remember { mutableStateOf(initialQuery) }
    val q = query.trim()
    val sections = remember(q) {
        if (q.isEmpty()) CONTAINER_GLOSSARY
        else CONTAINER_GLOSSARY.mapNotNull { section ->
            val hits = section.entries.filter {
                it.term.contains(q, ignoreCase = true) || it.definition.contains(q, ignoreCase = true)
            }
            if (hits.isEmpty()) null else section.copy(entries = hits)
        }
    }

    DeckConfigDialog(title = "What is all this?", onDismiss = onDismiss) {
        DeckTextRow(label = "Search", hint = "Filter terms", value = query, onValueChange = { query = it })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (sections.isEmpty()) {
                DeckConfigNote("No terms match \"$q\".")
            } else {
                sections.forEach { section ->
                    DeckGroup(section.title) {
                        section.entries.forEach { entry ->
                            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = entry.term,
                                    color = DeckPalette.onBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = entry.definition,
                                    color = DeckPalette.onSurfaceVariant,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        DeckButtonRow(onDismiss = onDismiss, confirmLabel = "Close", onConfirm = onDismiss)
    }
}
