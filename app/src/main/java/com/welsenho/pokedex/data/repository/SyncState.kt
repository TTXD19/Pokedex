package com.welsenho.pokedex.data.repository

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data object Complete : SyncState

    /**
     * [pokemonListUnavailable] means the DB has nothing at all to show (first launch
     * offline); [failedDetails] > 0 with a roster means partial content is on
     * screen and only some fetches need retrying.
     */
    data class Failed(val failedDetails: Int, val pokemonListUnavailable: Boolean) : SyncState
}
