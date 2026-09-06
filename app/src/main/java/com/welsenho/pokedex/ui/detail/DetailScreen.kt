package com.welsenho.pokedex.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.welsenho.pokedex.ui.components.OfflineBanner
import com.welsenho.pokedex.ui.components.PokemonImage
import com.welsenho.pokedex.ui.preview.PreviewData
import com.welsenho.pokedex.ui.theme.PokedexTheme

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onNavigateToPokemon: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailContent(
        state = state,
        onBack = onBack,
        onNavigateToPokemon = onNavigateToPokemon,
        onRetry = viewModel::load,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onNavigateToPokemon: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val pokemon = state.pokemon

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    pokemon?.let {
                        Text(
                            text = "#${it.id}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
                AnimatedVisibility(visible = !state.isOnline) {
                    OfflineBanner()
                }
            }
        },
        // Top and sides are consumed as fixed padding; the bottom inset is
        // handled inside the scrollable content instead.
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { padding ->
        if (pokemon == null) {
            // Not in the DB yet: an out-of-roster id being fetched, or offline.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (state.detailError) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Couldn't load this Pokémon.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                } else {
                    CircularProgressIndicator()
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                // Inside the scroll: content draws behind the nav bar but its
                // tail scrolls clear of it.
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PokemonImage(
                imageUrl = pokemon.imageUrl,
                contentDescription = pokemon.name,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(220.dp),
            )
            Text(
                text = pokemon.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                state.types.forEach { TypeChip(it) }
            }

            if (pokemon.evolvesFromName != null) {
                EvolvesFromRow(
                    name = pokemon.evolvesFromName,
                    imageUrl = state.evolvesFromImageUrl,
                    tappable = state.evolvesFromTappable,
                    onClick = { pokemon.evolvesFromId?.let(onNavigateToPokemon) },
                )
            }

            DescriptionSection(
                fetched = pokemon.speciesFetched,
                description = pokemon.description,
                error = state.speciesError,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun TypeChip(name: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EvolvesFromRow(
    name: String,
    imageUrl: String?,
    tappable: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clickable(enabled = tappable, onClick = onClick),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Evolves from",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        PokemonImage(
            imageUrl = imageUrl,
            contentDescription = name,
            modifier = Modifier.size(56.dp),
        )
    }
}

@Composable
private fun DescriptionSection(
    fetched: Boolean,
    description: String?,
    error: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Couldn't load the description.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRetry) { Text("Retry") }
            }

            !fetched -> CircularProgressIndicator(modifier = Modifier.size(24.dp))

            description != null -> Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---- Previews ----

@Preview(showBackground = true)
@Composable
private fun DetailPreview() {
    PokedexTheme {
        DetailContent(
            state = DetailUiState(
                pokemon = PreviewData.charizard,
                types = listOf("fire", "flying"),
                evolvesFromTappable = true,
            ),
            onBack = {},
            onNavigateToPokemon = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailEvolvesFromOutsideRosterPreview() {
    // Pikachu evolves from Pichu (#172), outside the 151: tappable (fetched on
    // demand), thumbnail blank until its detail has been cached.
    PokedexTheme {
        DetailContent(
            state = DetailUiState(
                pokemon = PreviewData.pikachu,
                types = listOf("electric"),
                evolvesFromTappable = true,
            ),
            onBack = {},
            onNavigateToPokemon = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailSpeciesLoadingPreview() {
    PokedexTheme {
        DetailContent(
            state = DetailUiState(
                pokemon = PreviewData.dratini,
                types = listOf("dragon"),
            ),
            onBack = {},
            onNavigateToPokemon = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailSpeciesErrorPreview() {
    PokedexTheme {
        DetailContent(
            state = DetailUiState(
                pokemon = PreviewData.dratini,
                types = listOf("dragon"),
                speciesError = true,
            ),
            onBack = {},
            onNavigateToPokemon = {},
            onRetry = {},
        )
    }
}
