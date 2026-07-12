package com.example.pundarapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pundarapp.ui.theme.ElectricBlue
import com.example.pundarapp.ui.theme.ElectricBlueDeep
import com.example.pundarapp.ui.theme.ElectricPurple
import com.example.pundarapp.ui.theme.SpaceDeep

/**
 * Profile avatar: shows [imageUrl] when available, otherwise [initials].
 */
@Composable
fun PundarAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    imageUrl: String? = null,
    showRing: Boolean = false,
    initialsFontSize: TextUnit = 14.sp,
    innerBackground: Color = SpaceDeep
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .then(
                if (showRing) {
                    Modifier
                        .background(
                            Brush.linearGradient(listOf(ElectricBlueDeep, ElectricPurple)),
                            CircleShape
                        )
                } else Modifier
            )
    ) {
        val innerSize = if (showRing) size - 4.dp else size
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(innerSize)
                .clip(CircleShape)
                .then(
                    if (showRing) Modifier.border(0.dp, Color.Transparent, CircleShape)
                    else Modifier
                )
                .background(innerBackground)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = initials,
                    color = ElectricBlue,
                    fontWeight = FontWeight.Black,
                    fontSize = initialsFontSize
                )
            }
        }
    }
}
