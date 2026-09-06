package com.welsenho.pokedex.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun OfflineBanner() {
    Surface(
        color = OfflineBannerBackground,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = OfflineBannerContent,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "You're offline. Showing saved data.",
                style = MaterialTheme.typography.titleSmall,
                color = OfflineBannerContent,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}

private val OfflineBannerBackground = Color(0xFFFFDAD6)
private val OfflineBannerContent = Color(0xFF93000A)
