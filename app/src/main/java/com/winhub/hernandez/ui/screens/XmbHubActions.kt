package com.winhub.hernandez.ui.screens

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Upload
import com.winhub.hernandez.container.Shortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** What only Home can do for a game: its own launch-method, settings, pin, container and delete flows. */
internal class HubXmbOps(
    val launchMethod: () -> Unit,
    val addToHome: () -> Unit,
    val containerProperties: () -> Unit,
    val delete: () -> Unit,
)

/**
 * The XMB options column for a Home game: the Games screen's full list (minus Community configs), with
 * Launch method, Add to Home Screen, Container properties and Delete running Home's own flows,
 * and Clone adding the copy to Home's library.
 */
internal fun hubXmbActions(
    context: Context,
    shortcut: Shortcut,
    game: HubGame,
    host: XmbGameHost,
    ops: HubXmbOps,
): List<XmbAction> = buildList {
    add(XmbAction("Launch method", Icons.Filled.Layers) { ops.launchMethod() })
    add(XmbAction("Settings", Icons.Filled.Settings, "Display, graphics, controller…", menu = { xmbSettingsMenu(it, shortcut, host) }))
    add(XmbAction("Game Details", Icons.Filled.Edit, "Name, genres, year, description", menu = { xmbGameDetailsMenu(it, shortcut) }))
    if (isSteamOriginShortcut(shortcut)) {
        add(XmbAction("Cloud Saves", Icons.Filled.CloudSync, "Steam Cloud sync — opens its own screen") { launchSaveManager(context, steamAppIdOf(shortcut)) })
    }
    if (isCustomShortcut(shortcut)) {
        add(XmbAction("Back up saves", Icons.Filled.Archive, menu = { xmbBackupSavesMenu(it, shortcut) }))
        add(XmbAction("Restore saves", Icons.Filled.Unarchive, menu = { xmbRestoreSavesMenu(it, shortcut) }))
    }
    add(XmbAction("Scrape cover", Icons.Filled.Search, menu = { xmbScrapeCoverMenu(it, shortcut) }))
    add(XmbAction("View logs", Icons.Filled.Description, menu = { xmbLogsMenu(it, shortcut) }))
    add(XmbAction("Clone to container", Icons.Filled.ContentCopy, menu = { hubCloneMenu(it, shortcut, game, host) }))
    add(XmbAction("Copy to Drive C…", Icons.Filled.DriveFileMove, menu = { xmbCopyToDriveCMenu(it, shortcut) }))
    add(XmbAction("Change executable…", Icons.Filled.SwapHoriz, menu = { xmbChangeExeMenu(it, shortcut) }))
    add(XmbAction("Add to Home Screen", Icons.Filled.Home) { ops.addToHome() })
    add(XmbAction("Export", Icons.Filled.Upload) { exportShortcut(context, shortcut) })
    add(XmbAction("Properties", Icons.Filled.Info, "Times played, playtime", menu = { xmbPropertiesMenu(it, shortcut) }))
    add(XmbAction("Container properties", Icons.Filled.Folder) { ops.containerProperties() })
    add(XmbAction("Delete", Icons.Filled.Delete, "Asks to confirm first", danger = true, menu = { hubDeleteMenu(it, game, ops) }))
}

private fun hubDeleteMenu(xmb: XmbScope, game: HubGame, ops: HubXmbOps): XmbMenu =
    XmbMenu("Delete game?", Icons.Filled.Delete, initialKey = "cancel") {
        listOf(
            XmbRow.Info("msg", "Delete “${game.name}”?", Icons.Filled.Info),
            XmbRow.Action("delete", "Delete", Icons.Filled.Delete, danger = true) {
                ops.delete()
                xmb.popToRoot()
            },
            XmbRow.Action("cancel", "Cancel", Icons.Filled.Close) { xmb.pop() },
        )
    }

/** Like the Games screen's clone, but the copy also joins Home's library (cloning into the same container changes nothing). */
private fun hubCloneMenu(xmb: XmbScope, s: Shortcut, game: HubGame, host: XmbGameHost): XmbMenu =
    XmbMenu("Select container", Icons.Filled.ContentCopy) {
        host.containers().mapIndexed { i, c ->
            XmbRow.Action("c${c.id}_$i", c.name, Icons.Filled.Folder, subtitle = if (c.id == s.container.id) "Current container" else null) {
                xmb.scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        val cloned = runCatching { s.cloneToContainer(c) }.getOrDefault(false)
                        if (cloned && c.id != s.container.id) {
                            HubGamesStore.add(
                                xmb.context,
                                game.copy(containerId = c.id, shortcutPath = File(c.getDesktopDir(), s.file.name).absolutePath),
                            )
                        }
                        cloned
                    }
                    xmb.toast(if (ok) "Shortcut cloned." else "Failed to clone shortcut.")
                    xmb.popToRoot()
                    if (ok) xmb.reloadGames()
                }
            }
        }
    }
