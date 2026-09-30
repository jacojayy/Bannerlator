package com.winlator.star.ui.screens

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.preference.PreferenceManager
import com.winlator.star.container.Container
import com.winlator.star.container.ContainerManager
import com.winlator.star.contents.AdrenotoolsManager
import com.winlator.star.contents.WrapperManager
import com.winlator.star.ui.Screen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** XMB column for the container list: one row per container, loaded on open. */
internal fun xmbContainersMenu(xmb: XmbScope, onEdit: (Int?) -> Unit): XmbMenu {
    val state = mutableStateOf<List<Container>>(emptyList())
    var loading = false
    return XmbMenu(
        title = "Containers",
        icon = Icons.Filled.Folder,
        rows = {
            if (!loading) {
                loading = true
                xmb.scope.launch(Dispatchers.IO) {
                    val manager = ContainerManager(xmb.context)
                    manager.reloadContainers()
                    state.value = manager.getContainers().toList()
                }
            }
            val xs = this
            val out = ArrayList<XmbRow>()
            val list = state.value
            out += XmbRow.Header(
                key = "hdr",
                label = if (list.isEmpty()) "No containers yet" else "${list.size} containers",
            )
            list.forEach { container ->
                out += XmbRow.Link(
                    key = "c${container.id}",
                    label = container.name,
                    icon = Icons.Filled.Folder,
                    value = "Edit",
                ) {
                    xmbContainerMenu(id = container.id, name = container.name, onEdit = onEdit)
                }
            }
            out += XmbRow.Action(key = "new", label = "New container", icon = Icons.Filled.Folder) {
                xs.pop()
                onEdit(null)
            }
            out
        },
    )
}

/** Per-container actions. [onEdit] opens the editor column. */
private fun xmbContainerMenu(id: Int, name: String, onEdit: (Int?) -> Unit): XmbMenu = XmbMenu(
    title = name,
    icon = Icons.Filled.Folder,
    rows = {
        val xs = this
        arrayListOf<XmbRow>(
            XmbRow.Header(key = "name", label = name),
            XmbRow.Action(key = "edit", label = "Edit container", icon = Icons.Filled.Settings) {
                xs.pop()
                onEdit(id)
            },
        )
    },
)

/** XMB file browser: directories are columns, files are read-only lines. */
internal fun xmbFileManagerMenu(dir: File): XmbMenu = XmbMenu(
    title = dir.name.ifEmpty { "File Manager" },
    icon = Icons.Filled.FolderOpen,
    rows = {
        val out = ArrayList<XmbRow>()
        out += XmbRow.Header(key = "path", label = dir.absolutePath)
        dir.parentFile?.let { parent ->
            out += XmbRow.Link(key = "up", label = "Up one level", icon = Icons.Filled.Folder) {
                xmbFileManagerMenu(parent)
            }
        }
        val entries = dir.listFiles()
        if (entries.isNullOrEmpty()) {
            out += XmbRow.Header(key = "empty", label = "Empty folder")
        } else {
            entries
                .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
                .forEach { file ->
                    if (file.isDirectory) {
                        out += XmbRow.Link(key = "d:${file.path}", label = file.name, icon = Icons.Filled.Folder) {
                            xmbFileManagerMenu(file)
                        }
                    } else {
                        out += XmbRow.Info(
                            key = "f:${file.path}",
                            label = file.name,
                            icon = Icons.Filled.FolderOpen,
                            value = humanSize(file.length()),
                        )
                    }
                }
        }
        out
    },
)

/** App settings as an XMB column: the frequent switches in place, the long tail behind a link. */
internal fun xmbSettingsMenu(xmb: XmbScope): XmbMenu {
    val prefs = PreferenceManager.getDefaultSharedPreferences(xmb.context)
    return XmbMenu(
        title = "Settings",
        icon = Icons.Filled.Settings,
        rows = {
            val xs = this
            val out = ArrayList<XmbRow>()
            out += XmbRow.Header(key = "app", label = "App")
            out += XmbRow.Toggle(
                key = "dark_mode",
                label = "Dark mode",
                icon = Icons.Filled.Settings,
                value = prefs.getBoolean("dark_mode", false),
            ) { enabled ->
                prefs.edit().putBoolean("dark_mode", enabled).apply()
                saved()
            }
            out += XmbRow.Toggle(
                key = "enable_big_picture_mode",
                label = "Big Picture mode",
                icon = Icons.Filled.Settings,
                value = prefs.getBoolean("enable_big_picture_mode", false),
            ) { enabled ->
                prefs.edit().putBoolean("enable_big_picture_mode", enabled).apply()
                saved()
            }
            out += XmbRow.Header(key = "more", label = "More")
            out += XmbRow.Link(key = "all", label = "All settings", icon = Icons.Filled.Settings) {
                screenMenu(Screen.Settings) { SettingsScreen(onSaved = { xs.pop() }) }
            }
            out
        },
    )
}

/** Graphics column: installed GPU drivers and wrapper slots as real XMB rows. */
internal fun xmbGraphicsMenu(xmb: XmbScope): XmbMenu {
    val drivers = mutableStateOf<List<String>>(emptyList())
    val slots = mutableStateOf<List<WrapperManager.WrapperSlot>>(emptyList())
    var loading = false
    return XmbMenu(
        title = "Graphics",
        icon = Icons.Filled.Memory,
        rows = {
            if (!loading) {
                loading = true
                xmb.scope.launch(Dispatchers.IO) {
                    val wrapperManager = WrapperManager(xmb.context)
                    slots.value = wrapperManager.listSlots()
                    val activity = xmb.context as? Activity
                    if (activity != null) {
                        val manager = AdrenotoolsManager(activity)
                        drivers.value = manager.enumarateInstalledDrivers().map { id ->
                            "${manager.getDriverName(id)} \u00b7 ${manager.getDriverVersion(id)}"
                        }
                    }
                }
            }
            val out = ArrayList<XmbRow>()
            out += XmbRow.Header(key = "gpu", label = "GPU drivers")
            val installed = drivers.value
            if (installed.isEmpty()) {
                out += XmbRow.Header(key = "gpuempty", label = "No drivers installed")
            } else {
                installed.forEachIndexed { index, label ->
                    out += XmbRow.Info(key = "d$index", label = label, icon = Icons.Filled.Memory)
                }
            }
            out += XmbRow.Header(key = "wrappers", label = "Wrappers")
            val wrapperSlots = slots.value
            if (wrapperSlots.isEmpty()) {
                out += XmbRow.Header(key = "wrempty", label = "No wrappers installed")
            } else {
                wrapperSlots.forEachIndexed { index, slot ->
                    out += XmbRow.Info(
                        key = "w$index",
                        label = slot.label,
                        icon = Icons.Filled.Layers,
                        value = slot.version,
                    )
                }
            }
            out
        },
    )
}

private fun humanSize(bytes: Long): String = when {
    bytes >= 1073741824L -> "%.1f GB".format(bytes / 1073741824.0)
    bytes >= 1048576L -> "%.1f MB".format(bytes / 1048576.0)
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
