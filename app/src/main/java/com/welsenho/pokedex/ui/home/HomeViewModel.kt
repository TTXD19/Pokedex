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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    private val repository: PokemonRepository,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeCaptures(),
            repository.observeTypeGroups(),
            repository.syncState,
            isRefreshing,
            networkMonitor.isOnline,
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
        sync()
        // Connectivity coming back is the best moment to finish an interrupted
        // sync — drop the initial emission so startup doesn't sync twice.
        viewModelScope.launch {
            networkMonitor.isOnline
                .distinctUntilChanged()
                .drop(1)
                .filter { it }
                .collect { repository.sync() }
        }
    }

    fun sync() {
        viewModelScope.launch { repository.sync() }
    }

    /**
     * Pull-to-refresh. When everything is already fetched, sync returns in
     * milliseconds — too fast for the indicator's show/hide animation, which
     * leaves it stuck. The floor keeps the true->false transition observable.
     */
    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            repository.sync()
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

    companion object {
        private const val MIN_REFRESH_VISIBLE_MS = 400L
    }
}
