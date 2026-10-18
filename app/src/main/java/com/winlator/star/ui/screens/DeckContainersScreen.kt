package com.winlator.star.ui.screens

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winlator.star.XServerDisplayActivity
import com.winlator.star.XrActivity
import com.winlator.star.container.Container
import com.winlator.star.core.FileUtils
import com.winlator.star.core.StringUtils
import com.winlator.star.store.deckTileFocus
import java.io.File

/**
 * Deck Mode's container list: the same containers and the same actions as the Containers tab
 * (run / edit / duplicate / remove / export / backup), rendered as DroidDeck cards.
 */
@Composable
internal fun DeckContainersScreen(onEditContainer: (Int) -> Unit, vm: ContainersViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as Activity
    val containers by vm.containers.collectAsState()
    val message by vm.message.collectAsState()
    var propertiesContainer by remember { mutableStateOf<Container?>(null) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.messageShown()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeckPalette.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
        ) {
            Text(
                text = "Storage",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DeckPalette.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${containers.size} installed",
                fontSize = 13.sp,
                color = DeckPalette.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeckPalette.primary)
                        .clickable { onEditContainer(-1) }
                        .padding(horizontal = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = DeckPalette.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "New",
                            color = DeckPalette.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeckPalette.surface)
                        .border(1.dp, DeckPalette.line, RoundedCornerShape(12.dp))
                        .clickable { onEditContainer(-2) }
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "Defaults",
                        color = DeckPalette.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (containers.isEmpty()) {
                Text(
                    text = "No containers yet.",
                    fontSize = 15.sp,
                    color = DeckPalette.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(containers, key = { it.id }) { container ->
                        DeckContainerCard(
                            name = container.name,
                            subtitle = listOf(container.wineVersion, container.screenSize)
                                .filter { it.isNotEmpty() }
                                .joinToString(" · "),
                            onRun = {
                                if (!XrActivity.isEnabled(context)) {
                                    val intent = Intent(context, XServerDisplayActivity::class.java)
                                    intent.putExtra("container_id", container.id)
                                    if (container.displayBackend ==
                                        Container.DISPLAY_BACKEND_WAYLAND
                                    ) {
                                        intent.putExtra("wayland_mode", true)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    XrActivity.openIntent(activity, container.id, null)
                                }
                            },
                            onEdit = { onEditContainer(container.id) },
                            onDuplicate = { vm.duplicate(container) {} },
                            onRemove = { vm.remove(container, context) {} },
                            onExport = {
                                vm.exportContainer(container) { path ->
                                    val msg = if (path != null) "Exported to $path"
                                    else "Export failed or already exists"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            onInfo = { propertiesContainer = container },
                            onBackupRestore = { /* Backup/Restore opens a fork flow in the normal tab */ },
                        )
                    }
                }
            }
        }
    }

    propertiesContainer?.let { current ->
        DeckPropertiesDialog(container = current, onDismiss = { propertiesContainer = null })
    }
}

/** One container as a DroidDeck card: 14dp surface, name + subtitle, blue play pill, overflow. */
@Composable
private fun DeckContainerCard(
    name: String,
    subtitle: String,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    onExport: () -> Unit,
    onInfo: () -> Unit,
    onBackupRestore: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(DeckPalette.groupShape)
            .background(DeckPalette.surface)
            .border(
                1.dp,
                if (focused) DeckPalette.primary else DeckPalette.line,
                DeckPalette.groupShape,
            )
            .deckTileFocus(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onEdit)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DeckPalette.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    color = DeckPalette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        // Run: the blue play pill (DroidDeck's PrimaryButton shape).
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 52.dp, height = 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DeckPalette.primary)
                .clickable { onRun() },
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Run",
                tint = DeckPalette.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(4.dp))

        Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { menuOpen = true },
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Actions",
                    tint = DeckPalette.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.background(DeckPalette.surfaceVariant),
            ) {
                DeckMenuItem("Edit", Icons.Filled.Edit) { menuOpen = false; onEdit() }
                DeckMenuItem("Duplicate", Icons.Filled.ContentCopy) { menuOpen = false; onDuplicate() }
                DeckMenuItem("Remove", Icons.Filled.Delete) { menuOpen = false; onRemove() }
                DeckMenuItem("Export", Icons.Filled.FileUpload) { menuOpen = false; onExport() }
                DeckMenuItem("Backup / Restore save", Icons.Filled.SettingsBackupRestore) { menuOpen = false; onBackupRestore() }
                DeckMenuItem("Properties", Icons.Filled.Info) { menuOpen = false; onInfo() }
            }
        }
    }
}

@Composable
private fun DeckMenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = DeckPalette.onBackground,
                fontSize = 14.sp,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeckPalette.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun DeckPropRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            color = DeckPalette.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeckPalette.onBackground,
        )
    }
}

/** DroidDeck-styled Properties sheet: identity header + live storage usage + Clear Cache. */
@Composable
internal fun DeckPropertiesDialog(container: Container, onDismiss: () -> Unit) {
    var driveCSize by remember { mutableStateOf(0L) }
    var cacheSize by remember { mutableStateOf(0L) }
    var cacheEpoch by remember { mutableStateOf(0) }
    val internalStorageSize = remember { FileUtils.getInternalStorageSize() }
    val handler = remember { Handler(Looper.getMainLooper()) }
    val totalSize = driveCSize + cacheSize
    val progress = if (internalStorageSize > 0)
        (totalSize.toFloat() / internalStorageSize).coerceIn(0f, 1f)
    else 0f

    LaunchedEffect(container, cacheEpoch) {
        driveCSize = 0
        cacheSize = 0
        FileUtils.getSizeAsync(File(container.rootDir, ".wine/drive_c")) { size ->
            handler.post { driveCSize += size }
        }
        FileUtils.getSizeAsync(File(container.rootDir, ".cache")) { size ->
            handler.post { cacheSize += size }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(DeckPalette.groupShape)
                .background(DeckPalette.surface)
                .border(1.dp, DeckPalette.line, DeckPalette.groupShape)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Properties",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeckPalette.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = container.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeckPalette.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(container.wineVersion, container.screenSize)
                        .filter { it.isNotEmpty() }
                        .joinToString(" · "),
                    fontSize = 12.5.sp,
                    color = DeckPalette.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DeckPalette.line),
                )
                Spacer(Modifier.height(4.dp))

                DeckPropRow("Drive C", StringUtils.formatBytes(driveCSize))
                DeckPropRow("Cache", StringUtils.formatBytes(cacheSize))
                DeckPropRow("Total", StringUtils.formatBytes(totalSize))

                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(DeckPalette.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DeckPalette.primary),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeckPalette.onBackground,
                    )
                }
                Text(
                    text = "Estimated used space",
                    fontSize = 11.sp,
                    color = DeckPalette.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(Modifier.height(18.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    DeckButton("Clear Cache", primary = false) {
                        FileUtils.clear(File(container.rootDir, ".cache"))
                        container.putExtra("desktopTheme", null)
                        container.saveData()
                        cacheEpoch++
                    }
                    Spacer(Modifier.weight(1f))
                    DeckButton("Close", primary = true) { onDismiss() }
                }
            }
        }
    }
}
