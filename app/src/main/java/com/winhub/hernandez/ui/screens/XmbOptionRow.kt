package com.winhub.hernandez.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winhub.hernandez.ui.theme.DangerRed

/** One row of the XMB options column, drawn the way the Deck XMB draws it: ringed icon, light label, subtitle when picked. */
@Composable
internal fun XmbOptionRow(
    label: String,
    icon: ImageVector,
    subtitle: String?,
    danger: Boolean,
    nested: Boolean,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val ring = if (danger) DangerRed else accent
    val boxShape = RoundedCornerShape(10.dp)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(34.dp)
                .clip(boxShape)
                .background(if (selected) ring.copy(alpha = 0.25f) else Color.Transparent)
                .then(if (selected) Modifier.border(1.5.dp, ring, boxShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, contentDescription = null, modifier = Modifier.size(19.dp),
                tint = when {
                    selected -> Color.White
                    danger -> Color(0xFFE8A197)
                    else -> Color(0xFFCFCFCF)
                },
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                if (nested) "$label  ›" else label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.58f),
                fontSize = if (selected) 16.sp else 13.5.sp,
                fontWeight = if (selected) FontWeight.Normal else FontWeight.Light,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (selected && subtitle != null) {
                Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
