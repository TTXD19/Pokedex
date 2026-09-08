package com.welsenho.pokedex.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.welsenho.pokedex.data.network.NetworkMonitor
import com.welsenho.pokedex.data.repository.PokemonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: PokemonRepository,
    private val networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        const val ARG_POKEMON_ID = "pokemonId"
    }

    private val pokemonId: Int = checkNotNull(savedStateHandle[ARG_POKEMON_ID])
    private val detailError = MutableStateFlow(false)
    private val speciesError = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> =
        combine(
            repository.observePokemon(pokemonId),
            repository.observeTypesOf(pokemonId),
            detailError,
            speciesError,
            networkMonitor.isOnline(),
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
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DetailUiState(),
        )

    init {
        loadPokemonData()
        reloadWhenBackOnline()
    }

    private fun reloadWhenBackOnline() {
        networkMonitor.connectivityRestored()
            .onEach { loadPokemonData() }
            .launchIn(viewModelScope)
    }

    fun loadPokemonData() {
        viewModelScope.launch {
            detailError.value = false
            speciesError.value = false
            val detailOk = repository.ensureDetail(pokemonId)
            detailError.value = !detailOk
            if (detailOk) {
                val speciesOk = repository.ensureSpecies(pokemonId)
                speciesError.value = !speciesOk
                repository.getPokemon(pokemonId)?.evolvesFromId
                    ?.let { repository.ensureDetail(it) }
            }
        }
    }
}
