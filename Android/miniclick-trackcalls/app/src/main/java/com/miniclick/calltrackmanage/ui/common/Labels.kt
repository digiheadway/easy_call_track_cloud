package com.miniclick.calltrackmanage.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Person

@Composable
fun LabelChip(
    label: String, 
    onClick: (() -> Unit)? = null,
    color: Color = MaterialTheme.colorScheme.tertiaryContainer,
    maxLines: Int = 1
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp),
        modifier = if (onClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else Modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (color == MaterialTheme.colorScheme.tertiaryContainer) 
                            MaterialTheme.colorScheme.onTertiaryContainer 
                        else Color.White,
                maxLines = maxLines,
                overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip
            )
        }
    }
}

@Composable
fun NoteChip(
    note: String,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    maxLines: Int = 1
) {
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp),
        modifier = if (onClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onClick) else Modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = contentColorFor(color).copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColorFor(color),
                maxLines = maxLines,
                overflow = if (maxLines == 1) TextOverflow.Ellipsis else TextOverflow.Clip
            )
        }
    }
}
