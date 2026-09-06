package com.welsenho.pokedex.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.welsenho.pokedex.data.local.PokemonEntity
import com.welsenho.pokedex.data.network.NetworkMonitor
import com.welsenho.pokedex.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val pokemon: PokemonEntity? = null,
    val types: List<String> = emptyList(),
    /** Species (description / evolves-from) fetch failed; offer retry. */
    val speciesError: Boolean = false,
    /**
     * Evolves-from is only tappable when the pre-evolution is one of our 151 —
     * e.g. Pikachu's pre-evolution Pichu (#172) is shown as plain text.
     */
    val evolvesFromTappable: Boolean = false,
    val evolvesFromImageUrl: String? = null,
    val isOnline: Boolean = true,
)

class DetailViewModel(
    private val repository: PokemonRepository,
    networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pokemonId: Int = checkNotNull(savedStateHandle[ARG_POKEMON_ID])

    private val speciesError = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> =
        combine(
            repository.observePokemon(pokemonId),
            repository.observeTypesOf(pokemonId),
            speciesError,
            networkMonitor.isOnline,
        ) { pokemon, types, error, online ->
            DetailUiState(pokemon = pokemon, types = types, speciesError = error, isOnline = online)
        }.map { state ->
            val evolvesFrom = state.pokemon?.evolvesFromId?.let { repository.getPokemon(it) }
            state.copy(
                evolvesFromTappable = evolvesFrom != null,
                evolvesFromImageUrl = evolvesFrom?.imageUrl,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )

    init {
        loadSpecies()
    }

    fun loadSpecies() {
        viewModelScope.launch {
            speciesError.value = false
            speciesError.value = !repository.ensureSpecies(pokemonId)
        }
    }

    companion object {
        const val ARG_POKEMON_ID = "pokemonId"
    }
}
