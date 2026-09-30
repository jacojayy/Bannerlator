package com.winlator.star.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Composites a normal tab (Containers, File Manager, Social Hub…) into an XMB column so it opens
 * in XMB mode instead of jumping to the tab. Keys are passed through ([passthrough]) so the
 * embedded screen keeps its own focus handling — B is still swallowed by the host to pop back.
 */
internal class XmbScreenPanel(
    private val content: @Composable () -> Unit,
) : XmbPanel {
    override val passthrough: Boolean get() = true

    @Composable
    override fun Content(modifier: Modifier) {
        Box(modifier = modifier) { content() }
    }
}
