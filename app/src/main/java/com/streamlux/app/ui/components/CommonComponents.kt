package com.streamlux.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.streamlux.app.ui.theme.PrimaryOrange

@Composable
fun StarRating(
    rating: Double, // 0 to 10 scale
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val starCount = 5
    val ratingInStars = rating / 2.0
    val size = if (compact) 14.dp else 18.dp
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        repeat(starCount) { index ->
            val starIndex = index + 1
            when {
                starIndex <= ratingInStars -> {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(size)
                    )
                }
                starIndex - ratingInStars < 1.0 -> {
                    Icon(
                        imageVector = Icons.Default.StarHalf,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(size)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(size)
                    )
                }
            }
        }
        
        if (!compact) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = String.format("%.1f", rating),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
