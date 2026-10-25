package com.winhub.hernandez.ui.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.winhub.hernandez.R
import com.winhub.hernandez.contents.WrapperManager
import com.winhub.hernandez.core.StringUtils
import com.winhub.hernandez.util.InAppFilePicker

/**
 * Deck-native wrapper manager, replacing the classic [WrapperManagerDialog] inside the Deck shortcut
 * editor. Same [WrapperManager] install pipeline, file picker ([InAppFilePicker]) and inspection/name
 * gate as the shared body — re-dressed with Deck widgets. The curated catalog browser is layered over
 * this dialog via the existing [WrapperCatalogPicker] so no download path is lost.
 */
@Composable
internal fun DeckWrapperManagerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { WrapperManager(context) }
    var slots by remember { mutableStateOf(manager.listSlots().toList()) }
    var imported by remember { mutableStateOf(manager.enumerateImported().toList()) }

    var pendingFileName by remember { mutableStateOf<String?>(null) }
    var importMode by remember { mutableStateOf(false) }
    var importNameDraft by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingInspection by remember { mutableStateOf<WrapperManager.Inspection?>(null) }
    var deleteTarget by remember { mutableStateOf<WrapperManager.Imported?>(null) }
    var confirmResetAll by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }

    fun refresh() {
        slots = manager.listSlots().toList()
        imported = manager.enumerateImported().toList()
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val target = pendingFileName
        val wasImport = importMode
        pendingFileName = null
        importMode = false
        if (result.resultCode == Activity.RESULT_OK) {
            val path = InAppFilePicker.pickedPath(result.data)
            val uri = result.data?.data ?: path?.let { InAppFilePicker.asUri(it) }
            if (uri != null) {
                if (wasImport) {
                    val inspection = manager.inspectUri(uri)
                    if (!inspection.valid) {
                        Toast.makeText(context, R.string.wrapper_invalid_toast, Toast.LENGTH_LONG).show()
                    } else {
                        val base = (path?.substringAfterLast('/') ?: uri.lastPathSegment?.substringAfterLast('/'))
                            ?.substringBeforeLast('.')
                            ?.takeIf { it.isNotBlank() }
                            ?: "Imported wrapper"
                        importNameDraft = base
                        pendingInspection = inspection
                        pendingImportUri = uri
                    }
                } else if (target != null) {
                    if (manager.installOverride(target, uri)) {
                        Toast.makeText(context, R.string.wrapper_updated_toast, Toast.LENGTH_SHORT).show()
                        refresh()
                    } else {
                        Toast.makeText(context, R.string.wrapper_invalid_toast, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun browse(target: String?, asImport: Boolean) {
        pendingFileName = target
        importMode = asImport
        filePicker.launch(
            InAppFilePicker.buildIntent(
                context,
                InAppFilePicker.WRAPPER,
                context.getString(R.string.wrapper_select_title),
            )
        )
    }

    DeckConfigDialog(title = context.getString(R.string.wrapper_manager_title), onDismiss = onDismiss) {
        DeckConfigNote(context.getString(R.string.wrapper_manager_header))

        DeckGroup("Bundled wrappers") {
            slots.forEach { slot ->
                DeckRow(
                    label = slot.label,
                    hint = if (slot.isOverridden) "Custom · ${slot.version}" else slot.version,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeckConfigChip(context.getString(R.string.wrapper_update), selected = false) {
                            browse(slot.fileName, false)
                        }
                        if (slot.isOverridden) {
                            DeckConfigChip("Reset", selected = false) {
                                manager.removeOverride(slot.fileName)
                                Toast.makeText(context, R.string.wrapper_reset_toast, Toast.LENGTH_SHORT).show()
                                refresh()
                            }
                        }
                    }
                }
            }
        }

        if (imported.isNotEmpty()) {
            DeckGroup("Imported wrappers") {
                imported.forEach { imp ->
                    DeckRow(label = imp.label, hint = imp.identifier) {
                        DeckConfigChip(context.getString(R.string.wrapper_delete), selected = false) {
                            deleteTarget = imp
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DeckButton("Import wrapper", primary = true) { browse(null, true) }
            DeckButton("Download", primary = false) { showCatalog = true }
            DeckButton(context.getString(R.string.wrapper_reset_all), primary = false) { confirmResetAll = true }
        }
    }

    if (showCatalog) {
        WrapperCatalogPicker(
            isQualcomm = false,
            gpuModel = "this GPU",
            onDismiss = { showCatalog = false },
            onInstalled = { refresh() },
        )
    }

    val importUri = pendingImportUri
    val inspection = pendingInspection
    if (importUri != null && inspection != null) {
        DeckConfigDialog(
            title = context.getString(R.string.wrapper_import_title).removeSuffix("…"),
            onDismiss = { pendingImportUri = null; pendingInspection = null },
        ) {
            DeckConfigNote("Version: ${inspection.version}" + if (inspection.notes.isNotBlank()) " · ${inspection.notes}" else "")
            if (inspection.envKeys.isNotEmpty()) {
                DeckConfigNote("Detected settings (${inspection.envKeys.size}): ${inspection.envKeys.joinToString(", ")}")
            }
            DeckConfigTextField(
                label = context.getString(R.string.wrapper_import_name_message),
                value = importNameDraft,
                onValueChange = { importNameDraft = it },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeckButton("Cancel", primary = false) { pendingImportUri = null; pendingInspection = null }
                DeckButton("Import", primary = true) {
                    val name = importNameDraft.trim()
                    val id = StringUtils.parseIdentifier(name)
                    when {
                        name.isEmpty() ->
                            Toast.makeText(context, R.string.wrapper_import_name_empty_toast, Toast.LENGTH_SHORT).show()
                        manager.isReservedIdentifier(id) ->
                            Toast.makeText(context, R.string.wrapper_import_reserved_toast, Toast.LENGTH_LONG).show()
                        else -> {
                            pendingImportUri = null
                            pendingInspection = null
                            if (manager.importWrapper(importUri, name) != null) {
                                Toast.makeText(context, R.string.wrapper_imported_toast, Toast.LENGTH_SHORT).show()
                                refresh()
                            } else {
                                Toast.makeText(context, R.string.wrapper_invalid_toast, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    val toDelete = deleteTarget
    if (toDelete != null) {
        DeckConfigDialog(
            title = context.getString(R.string.wrapper_delete_confirm_title),
            onDismiss = { deleteTarget = null },
        ) {
            DeckConfigNote(context.getString(R.string.wrapper_delete_confirm_message))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeckButton("Cancel", primary = false) { deleteTarget = null }
                DeckButton(context.getString(R.string.wrapper_delete), primary = true) {
                    manager.deleteImported(toDelete.identifier)
                    deleteTarget = null
                    Toast.makeText(context, R.string.wrapper_reset_toast, Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
        }
    }

    if (confirmResetAll) {
        DeckConfigDialog(
            title = context.getString(R.string.wrapper_reset_all_confirm_title),
            onDismiss = { confirmResetAll = false },
        ) {
            DeckConfigNote(context.getString(R.string.wrapper_reset_all_confirm_message))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeckButton("Cancel", primary = false) { confirmResetAll = false }
                DeckButton(context.getString(R.string.wrapper_reset_all), primary = true) {
                    manager.resetAll()
                    confirmResetAll = false
                    Toast.makeText(context, R.string.wrapper_reset_toast, Toast.LENGTH_SHORT).show()
                    refresh()
                }
            }
        }
    }
}
