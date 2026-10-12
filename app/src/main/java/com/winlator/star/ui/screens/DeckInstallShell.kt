package com.winlator.star.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Shared scaffold for the in-Deck install screens (GOG / Epic / Amazon / Steam). Draws Back row,
 * cover art, title, developer, then the install state machine: idle shows Install (plus the
 * "Open in <store>" escape hatch a caller can offer when the game is not in the library), running
 * shows progress + status + Cancel with an optional Pause/Resume, plus the done and error states.
 * Every store screen feeds the same inputs; the extra handles default off.
 */
@Composable
internal fun DeckInstallShell(
    title: String,
    art: String?,
    dev: String,
    onBack: () -> Unit,
    progress: Int,
    status: String,
    finished: Boolean,
    error: String?,
    started: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    canInstall: Boolean = true,
    onPauseResume: (() -> Unit)? = null,
    paused: Boolean = false,
    onOpenStore: (() -> Unit)? = null,
    storeLabel: String = "",
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Deck.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onBack() }.padding(vertical = 4.dp),
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = Deck.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text("Back", color = Deck.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))

        val shape = RoundedCornerShape(12.dp)
        if (!art.isNullOrEmpty()) {
            AsyncImage(
                model = art,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(shape)
                    .background(Deck.surface)
                    .border(1.dp, Deck.line, shape),
            )
            Spacer(Modifier.size(12.dp))
        }
        Text(
            text = title,
            color = Deck.onBackground,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
        )
        if (dev.isNotEmpty()) {
            Text(dev, color = Deck.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.size(18.dp))

        if (finished) {
            Text("Installed.", color = Deck.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            return@Column
        }
        if (started && error == null) {
            LinearProgressIndicator(
                progress = { (progress.coerceIn(0, 100)) / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Deck.primary,
                trackColor = Deck.surfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
            Text(status, color = Deck.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DeckButton("Cancel", primary = false) { onCancel() }
                if (onPauseResume != null) {
                    DeckButton(if (paused) "Resume" else "Pause", primary = false) { onPauseResume() }
                }
            }
        } else {
            if (error != null) {
                Text(error, color = Color(0xFFFF6B6B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(10.dp))
            }
            if (canInstall) {
                DeckButton("Install", primary = true) { onStart() }
            }
            if (onOpenStore != null) {
                if (canInstall) Spacer(Modifier.size(8.dp))
                DeckButton("Open in $storeLabel", primary = !canInstall) { onOpenStore() }
            }
        }
    }
}
