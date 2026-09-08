package com.welsenho.pokedex.data.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

interface NetworkMonitor {
    fun isOnline(): Flow<Boolean>
    /**
     * Fires once each time connectivity comes back after being lost. The
     * state at subscription time is skipped so subscribing while already
     * online does not fire.
     */
    fun connectivityRestored(): Flow<Unit> = isOnline()
        .distinctUntilChanged()
        .drop(1)
        .filter { it }
        .map { }
}
