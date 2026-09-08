package com.welsenho.pokedex.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.welsenho.pokedex.data.network.NetworkMonitor
import com.welsenho.pokedex.data.repository.PokemonRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val repository: PokemonRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    companion object {
        private const val MIN_REFRESH_VISIBLE_MS = 400L
    }

    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeCaptures(),
            repository.observeTypeGroups(),
            repository.syncState,
            isRefreshing,
            networkMonitor.isOnline(),
        ) { captures, groups, sync, refreshing, online ->
            HomeUiState(
                captured = captures.map {
                    HomeUiState.Companion.CapturedItem(
                        captureId = it.capture.id,
                        pokemonId = it.pokemon.id,
                        name = it.pokemon.name,
                        imageUrl = it.pokemon.imageUrl,
                    )
                },
                typeGroups = groups,
                syncState = sync,
                isRefreshing = refreshing,
                isOnline = online,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    init {
        syncPokemonData()
        syncWhenBackOnline()
    }

    private fun syncWhenBackOnline() {
        networkMonitor.connectivityRestored()
            .onEach { repository.syncPokemonData() }
            .launchIn(viewModelScope)
    }

    fun syncPokemonData() {
        viewModelScope.launch { repository.syncPokemonData() }
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.syncPokemonData()
            delay(MIN_REFRESH_VISIBLE_MS.milliseconds)
            isRefreshing.value = false
        }
    }

    fun capture(pokemonId: Int) {
        viewModelScope.launch { repository.capture(pokemonId) }
    }

    fun release(captureId: Long) {
        viewModelScope.launch { repository.release(captureId) }
    }
}
