package com.welsenho.pokedex.testing

import com.welsenho.pokedex.data.local.CaptureEntity
import com.welsenho.pokedex.data.local.CaptureWithPokemon
import com.welsenho.pokedex.data.local.PokemonEntity
import com.welsenho.pokedex.data.repository.PokemonRepository
import com.welsenho.pokedex.data.repository.SyncState
import com.welsenho.pokedex.data.repository.TypeGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory PokemonRepository. Behaves like the real one from the ViewModels'
 * point of view: capture/release mutate state that the observe* flows re-emit.
 */
class FakePokemonRepository : PokemonRepository {

    val pokemonById = MutableStateFlow<Map<Int, PokemonEntity>>(emptyMap())
    val typeGroups = MutableStateFlow<List<TypeGroup>>(emptyList())
    val captures = MutableStateFlow<List<CaptureEntity>>(emptyList())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState

    var syncCalls = 0
        private set
    var ensureSpeciesCalls = 0
        private set
    var ensureSpeciesResult = true
    var ensureDetailCalls = 0
        private set
    var ensureDetailResult = true
    private var nextCaptureId = 1L
    private var now = 0L

    fun setSyncState(state: SyncState) {
        _syncState.value = state
    }

    fun addPokemon(pokemon: PokemonEntity) {
        pokemonById.value += (pokemon.id to pokemon)
    }

    override fun observeTypeGroups(): Flow<List<TypeGroup>> = typeGroups

    override fun observeCaptures(): Flow<List<CaptureWithPokemon>> =
        captures.map { list ->
            list.sortedWith(compareByDescending<CaptureEntity> { it.capturedAt }.thenByDescending { it.id })
                .map { CaptureWithPokemon(it, pokemonById.value.getValue(it.pokemonId)) }
        }

    override fun observePokemon(id: Int): Flow<PokemonEntity?> =
        pokemonById.map { it[id] }

    override fun observeTypesOf(id: Int): Flow<List<String>> =
        typeGroups.map { groups -> groups.filter { g -> g.pokemon.any { it.id == id } }.map { it.name } }

    override suspend fun getPokemon(id: Int): PokemonEntity? = pokemonById.value[id]

    override suspend fun capture(pokemonId: Int) {
        captures.value += CaptureEntity(id = nextCaptureId++, pokemonId = pokemonId, capturedAt = ++now)
    }

    override suspend fun release(captureId: Long) {
        captures.value = captures.value.filterNot { it.id == captureId }
    }

    override suspend fun syncPokemonData() {
        syncCalls++
    }

    override suspend fun ensureSpecies(id: Int): Boolean {
        ensureSpeciesCalls++
        return ensureSpeciesResult
    }

    override suspend fun ensureDetail(id: Int): Boolean {
        ensureDetailCalls++
        if (!ensureDetailResult) return false
        if (!pokemonById.value.containsKey(id)) {
            addPokemon(PokemonEntity(id = id, name = "pokemon-$id", detailFetched = true))
        }
        return true
    }
}
