package com.welsenho.pokedex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.welsenho.pokedex.ui.theme.PokedexTheme

/**
 * One cell in a horizontal Pokémon row: artwork with a Pokéball action button
 * overlaid top-end (capture in the collection, release in My Pocket) and the
 * name underneath, matching the reference mock.
 */
@Composable
fun PokemonCard(
    name: String,
    imageUrl: String?,
    onClick: () -> Unit,
    onBallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            PokemonImage(
                imageUrl = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(80.dp)
                    .clickable(onClick = onClick),
            )
            Pokeball(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp),
                onClick = onBallClick,
            )
        }
        Text(
            text = name.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
fun Pokeball(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        val radius = size.minDimension / 2f
        drawCircle(Color.White, radius = radius)
        drawArc(
            color = Color(0xFFE3350D),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
        )
        drawLine(
            color = Color(0xFF1F1F1F),
            start = center.copy(x = 0f),
            end = center.copy(x = size.width),
            strokeWidth = radius * 0.18f,
        )
        drawCircle(Color(0xFF1F1F1F), radius = radius * 0.34f)
        drawCircle(Color.White, radius = radius * 0.18f)
        drawCircle(Color(0xFF1F1F1F), radius = radius, style = Stroke(width = radius * 0.12f))
    }
}

@Preview(showBackground = true)
@Composable
private fun PokemonCardPreview() {
    PokedexTheme {
        PokemonCard(
            name = "charizard",
            imageUrl = null,
            onClick = {},
            onBallClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PokeballPreview() {
    PokedexTheme {
        Pokeball(modifier = Modifier.size(48.dp), onClick = {})
    }
}
