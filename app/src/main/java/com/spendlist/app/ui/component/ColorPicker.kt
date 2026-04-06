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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AVAILABLE_COLORS: List<Long> = listOf(
    0xFFFF6B35, // Orange
    0xFF4ECDC4, // Teal
    0xFFFF6B6B, // Red
    0xFF45B7D1, // Blue
    0xFF96CEB4, // Green
    0xFFFFEAA7, // Yellow
    0xFFDDA0DD, // Plum
    0xFF95A5A6, // Grey
    0xFFE74C3C, // Bright Red
    0xFF3498DB, // Bright Blue
    0xFF2ECC71, // Emerald
    0xFF9B59B6, // Purple
    0xFFF39C12, // Amber
    0xFF1ABC9C, // Turquoise
    0xFFE91E63, // Pink
    0xFF607D8B, // Blue Grey
)

@Composable
fun ColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AVAILABLE_COLORS) { colorValue ->
            val isSelected = colorValue == selectedColor
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(colorValue))
                    .then(
                        if (isSelected) Modifier.border(
                            3.dp,
                            MaterialTheme.colorScheme.onSurface,
                            CircleShape
                        )
                        else Modifier
                    )
                    .clickable { onColorSelected(colorValue) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
