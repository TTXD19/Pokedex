package com.welsenho.pokedex.data.repository

import com.welsenho.pokedex.data.local.CaptureWithPokemon
import com.welsenho.pokedex.data.local.PokemonEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for Pokémon data. Reads always come from the local DB;
 * [sync] and [ensureSpecies] pull from the network into it.
 */
interface PokemonRepository {

    val syncState: StateFlow<SyncState>

    /** Categories alphabetical, Pokémon within a category by id (per the mock). */
    fun observeTypeGroups(): Flow<List<TypeGroup>>

    /** Most recent capture first. */
    fun observeCaptures(): Flow<List<CaptureWithPokemon>>

    fun observePokemon(id: Int): Flow<PokemonEntity?>

    fun observeTypesOf(id: Int): Flow<List<String>>

    /** One-shot read; null when the id is outside our roster (e.g. Pichu #172). */
    suspend fun getPokemon(id: Int): PokemonEntity?

    /** Records one capture event; the same Pokémon can be captured repeatedly. */
    suspend fun capture(pokemonId: Int)

    /** Releases one specific capture event, not every capture of that species. */
    suspend fun release(captureId: Long)

    /**
     * Resumable sync: fetches the roster if missing, then only the details not
     * yet in the DB, committing each success immediately.
     */
    suspend fun sync()

    /**
     * Fetches species data (description / evolves-from) if not cached yet.
     * Returns false on failure so the caller can offer a retry.
     */
    suspend fun ensureSpecies(id: Int): Boolean
}
