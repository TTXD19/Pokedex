package com.welsenho.pokedex.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.welsenho.pokedex.data.repository.SyncState
import com.welsenho.pokedex.data.repository.TypeGroup
import com.welsenho.pokedex.ui.components.PokemonCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPokemonClick: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.primary)
            )
        },
    ) { padding ->
        when {
            state.showFullScreenError -> FullScreenError(
                onRetry = viewModel::sync,
                modifier = Modifier.padding(padding),
            )

            state.typeGroups.isEmpty() && state.captured.isEmpty() &&
                state.syncState !is SyncState.Failed -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            else -> HomeContent(
                state = state,
                onPokemonClick = onPokemonClick,
                onCapture = viewModel::capture,
                onRelease = viewModel::release,
                onRetry = viewModel::sync,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onPokemonClick: (Int) -> Unit,
    onCapture: (Int) -> Unit,
    onRelease: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        val failed = state.syncState as? SyncState.Failed
        if (failed != null && !failed.rosterUnavailable) {
            item(key = "sync_banner") {
                SyncBanner(failedCount = failed.failedDetails, onRetry = onRetry)
            }
        }

        item(key = "pocket_header") {
            SectionHeader(title = "My Pocket", count = state.captured.size)
        }
        item(key = "pocket_row") {
            if (state.captured.isEmpty()) {
                Text(
                    text = "Tap a Pokéball below to capture a Pokémon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            } else {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.captured, key = { it.captureId }) { item ->
                        PokemonCard(
                            name = item.name,
                            imageUrl = item.imageUrl,
                            onClick = { onPokemonClick(item.pokemonId) },
                            onBallClick = { onRelease(item.captureId) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        state.typeGroups.forEach { group ->
            item(key = "header_${group.name}") {
                SectionHeader(
                    title = group.name.replaceFirstChar { it.uppercase() },
                    count = group.pokemon.size,
                )
            }
            item(key = "row_${group.name}") {
                TypeRow(group = group, onPokemonClick = onPokemonClick, onCapture = onCapture)
            }
        }
    }
}

@Composable
private fun TypeRow(
    group: TypeGroup,
    onPokemonClick: (Int) -> Unit,
    onCapture: (Int) -> Unit,
) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(group.pokemon, key = { it.id }) { pokemon ->
            PokemonCard(
                name = pokemon.name,
                imageUrl = pokemon.imageUrl,
                onClick = { onPokemonClick(pokemon.id) },
                onBallClick = { onCapture(pokemon.id) },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(text = "$count", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SyncBanner(failedCount: Int, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = "$failedCount Pokémon failed to load.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun FullScreenError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Couldn't load Pokémon.", style = MaterialTheme.typography.titleMedium)
        Text(
            "Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Button(onClick = onRetry) { Text("Retry") }
    }
}
