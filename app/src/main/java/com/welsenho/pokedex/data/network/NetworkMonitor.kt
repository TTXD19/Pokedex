package com.welsenho.pokedex.data.network

import kotlinx.coroutines.flow.Flow

/** Emits the device's connectivity as it changes; first emission is immediate. */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
