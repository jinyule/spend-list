package com.spendlist.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class IconOption(val name: String, val icon: ImageVector)

val AVAILABLE_ICONS = listOf(
    IconOption("SmartToy", Icons.Default.Face),
    IconOption("Dns", Icons.Default.Menu),
    IconOption("SportsEsports", Icons.Default.Star),
    IconOption("Build", Icons.Default.Build),
    IconOption("Cloud", Icons.Default.Cloud),
    IconOption("Language", Icons.Default.Language),
    IconOption("Storage", Icons.Default.Storage),
    IconOption("MoreHoriz", Icons.Default.MoreVert),
    IconOption("PhoneAndroid", Icons.Default.Phone),
    IconOption("Email", Icons.Default.Email),
    IconOption("ShoppingCart", Icons.Default.ShoppingCart),
    IconOption("School", Icons.Default.School),
    IconOption("Favorite", Icons.Default.Favorite),
    IconOption("MusicNote", Icons.Default.Notifications),
    IconOption("CameraAlt", Icons.Default.AccountCircle),
    IconOption("Wifi", Icons.Default.Wifi),
    IconOption("Lock", Icons.Default.Lock),
    IconOption("Code", Icons.Default.Code),
    IconOption("Paid", Icons.Default.Paid),
    IconOption("Rocket", Icons.Default.RocketLaunch),
)

fun getIconByName(name: String): ImageVector {
    return AVAILABLE_ICONS.find { it.name == name }?.icon ?: Icons.Default.Star
}

@Composable
fun IconPicker(
    selectedIconName: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AVAILABLE_ICONS) { option ->
            val isSelected = option.name == selectedIconName
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                        else Modifier
                    )
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onIconSelected(option.name) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.name,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
