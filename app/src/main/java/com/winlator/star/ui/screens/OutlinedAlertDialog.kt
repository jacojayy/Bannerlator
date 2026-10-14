package com.winlator.star.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * True while a config dialog is being composed INSIDE an expanding Deck row instead of floating as
 * a popup. The Deck editor sets it so the classic dialogs render as an inline bordered card that
 * pushes the rows below it down — same fields, same design, no re-implementation. It flips back off
 * for the dialog's own title/text slots so any NESTED dialog (extension picker, backups, …) opened
 * from inside still pops up normally.
 */
internal val LocalInlineDialog = staticCompositionLocalOf { false }

// Single source of truth for the app's AlertDialogs: a drop-in wrapper around the Material3
// slot-based AlertDialog that adds a 1dp outline around the whole popup box — matching the
// outline that Modifier.outlinedMenuCard() (MenuStyle.kt) puts on the app's menus. Signature
// mirrors androidx.compose.material3.AlertDialog exactly (compose-bom 2024.02.00 → material3
// 1.2.0), so every call site can swap AlertDialog( → OutlinedAlertDialog( unchanged. The
// border shares the dialog's shape so its corner radius follows the rounded dialog.
@Composable
internal fun OutlinedAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties()
) {
    if (LocalInlineDialog.current) {
        // Inline mode: the same slot content as a bordered card in the row flow, no Dialog window.
        // Slots run under provides(false) so nested OutlinedAlertDialog calls pop up as before.
        Surface(
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, shape),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                CompositionLocalProvider(LocalInlineDialog provides false) {
                    icon?.invoke()
                    if (title != null) {
                        title()
                        Spacer(Modifier.padding(top = 4.dp))
                    }
                    if (text != null) {
                        text()
                        Spacer(Modifier.padding(top = 10.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                        Spacer(Modifier.width(8.dp))
                    }
                    confirmButton()
                }
            }
        }
        return
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape),
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties
    )
}
