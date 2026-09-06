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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val pokemon: PokemonEntity? = null,
    val types: List<String> = emptyList(),
    /** The Pokémon itself couldn't be fetched (out-of-roster id, offline). */
    val detailError: Boolean = false,
    /** Species (description / evolves-from) fetch failed; offer retry. */
    val speciesError: Boolean = false,
    /** Pre-evolutions outside the 151 are fetched on demand, so always tappable. */
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

    private val detailError = MutableStateFlow(false)
    private val speciesError = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> =
        combine(
            repository.observePokemon(pokemonId),
            repository.observeTypesOf(pokemonId),
            detailError,
            speciesError,
            networkMonitor.isOnline,
        ) { pokemon, types, dError, sError, online ->
            DetailUiState(
                pokemon = pokemon,
                types = types,
                detailError = dError,
                speciesError = sError,
                isOnline = online,
            )
        }.map { state ->
            val evolvesFrom = state.pokemon?.evolvesFromId?.let { repository.getPokemon(it) }
            state.copy(
                evolvesFromTappable = state.pokemon?.evolvesFromId != null,
                evolvesFromImageUrl = evolvesFrom?.imageUrl,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )

    init {
        load()
        // Connectivity coming back retries whatever failed automatically;
        // when everything is cached this is DB checks only, no requests.
        viewModelScope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .drop(1)
                .filter { it }
                .collect { load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            detailError.value = false
            speciesError.value = false
            // Detail first: out-of-roster ids (e.g. Igglybuff #174) have no row
            // yet, and the species write needs one to land in.
            val detailOk = repository.ensureDetail(pokemonId)
            detailError.value = !detailOk
            if (detailOk) {
                speciesError.value = !repository.ensureSpecies(pokemonId)
                // Prefetch the pre-evolution so its thumbnail shows and
                // tapping through is instant.
                repository.getPokemon(pokemonId)?.evolvesFromId
                    ?.let { repository.ensureDetail(it) }
            }
        }
    }

    companion object {
        const val ARG_POKEMON_ID = "pokemonId"
    }
}
