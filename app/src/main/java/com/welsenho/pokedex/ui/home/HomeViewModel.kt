package com.welsenho.pokedex.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.welsenho.pokedex.PokedexApplication
import com.welsenho.pokedex.data.repository.PokemonRepository
import com.welsenho.pokedex.data.repository.SyncState
import com.welsenho.pokedex.data.repository.TypeGroup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CapturedItem(
    val captureId: Long,
    val pokemonId: Int,
    val name: String,
    val imageUrl: String?,
)

data class HomeUiState(
    val captured: List<CapturedItem> = emptyList(),
    val typeGroups: List<TypeGroup> = emptyList(),
    val syncState: SyncState = SyncState.Idle,
) {
    /** First launch with nothing fetched and the roster unreachable. */
    val showFullScreenError: Boolean
        get() = typeGroups.isEmpty() &&
            (syncState as? SyncState.Failed)?.rosterUnavailable == true
}

class HomeViewModel(private val repository: PokemonRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeCaptures(),
            repository.observeTypeGroups(),
            repository.syncState,
        ) { captures, groups, sync ->
            HomeUiState(
                captured = captures.map {
                    CapturedItem(
                        captureId = it.capture.id,
                        pokemonId = it.pokemon.id,
                        name = it.pokemon.name,
                        imageUrl = it.pokemon.imageUrl,
                    )
                },
                typeGroups = groups,
                syncState = sync,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    init {
        sync()
    }

    fun sync() {
        viewModelScope.launch { repository.sync() }
    }

    fun capture(pokemonId: Int) {
        viewModelScope.launch { repository.capture(pokemonId) }
    }

    fun release(captureId: Long) {
        viewModelScope.launch { repository.release(captureId) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PokedexApplication
                HomeViewModel(app.container.pokemonRepository)
            }
        }
    }
}
