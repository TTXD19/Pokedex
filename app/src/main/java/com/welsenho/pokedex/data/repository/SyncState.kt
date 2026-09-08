package com.welsenho.pokedex.data.repository

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data object Complete : SyncState
    data class Failed(val failedDetails: Int, val pokemonListUnavailable: Boolean) : SyncState
}
