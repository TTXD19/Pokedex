package com.welsenho.pokedex.data.repository

import com.welsenho.pokedex.data.local.CaptureDao
import com.welsenho.pokedex.data.local.CaptureEntity
import com.welsenho.pokedex.data.local.CaptureWithPokemon
import com.welsenho.pokedex.data.local.PokemonDao
import com.welsenho.pokedex.data.local.PokemonEntity
import com.welsenho.pokedex.data.local.PokemonTypeEntity
import com.welsenho.pokedex.data.remote.PokeApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

class PokemonRepositoryImpl(
    private val pokemonDao: PokemonDao,
    private val captureDao: CaptureDao,
    private val api: PokeApiService,
    private val clock: () -> Long = System::currentTimeMillis,
) : PokemonRepository {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val syncMutex = Mutex()

    // ---- Reads (DB is the single source of truth; UI only ever observes it) ----

    override fun observeTypeGroups(): Flow<List<TypeGroup>> =
        pokemonDao.observeTypeRows().map { rows ->
            rows.groupBy { it.typeName }
                .map { (type, group) -> TypeGroup(type, group.map { it.pokemon }) }
        }

    override fun observeCaptures(): Flow<List<CaptureWithPokemon>> = captureDao.observeAll()

    override fun observePokemon(id: Int): Flow<PokemonEntity?> = pokemonDao.observePokemon(id)

    override fun observeTypesOf(id: Int): Flow<List<String>> = pokemonDao.observeTypesOf(id)

    override suspend fun pokemonExists(id: Int): Boolean = pokemonDao.exists(id)

    // ---- Capture / release ----

    override suspend fun capture(pokemonId: Int) {
        captureDao.insert(CaptureEntity(pokemonId = pokemonId, capturedAt = clock()))
    }

    override suspend fun release(captureId: Long) {
        captureDao.delete(captureId)
    }

    // ---- Sync ----

    /**
     * The roster fetch seeds one row per Pokémon, then every row still missing
     * detail is fetched with bounded concurrency. Each success is committed to
     * the DB immediately, so the UI grows as data arrives and an interrupted
     * run resumes from wherever it died. Already-fetched rows are never
     * re-requested.
     */
    override suspend fun sync() {
        // A second caller (e.g. retry spam) just waits for the run in flight.
        syncMutex.withLock {
            _syncState.value = SyncState.Running

            if (pokemonDao.count() < POKEMON_LIMIT) {
                try {
                    val roster = api.getPokemonList(limit = POKEMON_LIMIT)
                    pokemonDao.insertRoster(
                        roster.results.map { PokemonEntity(id = it.id, name = it.name) }
                    )
                } catch (e: Exception) {
                    if (pokemonDao.count() == 0) {
                        _syncState.value = SyncState.Failed(0, rosterUnavailable = true)
                        return
                    }
                    // Partial roster can't happen (single request) — but a stale
                    // shorter roster from a previous limit is still usable.
                }
            }

            val missing = pokemonDao.missingDetailIds()
            val failed = coroutineScope {
                val semaphore = Semaphore(MAX_CONCURRENT_DETAIL_FETCHES)
                missing.map { id ->
                    async {
                        semaphore.withPermit {
                            try {
                                fetchAndStoreDetail(id)
                                null
                            } catch (e: Exception) {
                                id
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            _syncState.value =
                if (failed.isEmpty()) SyncState.Complete
                else SyncState.Failed(failed.size, rosterUnavailable = false)
        }
    }

    private suspend fun fetchAndStoreDetail(id: Int) {
        val detail = api.getPokemonDetail(id)
        pokemonDao.applyDetail(
            id = detail.id,
            name = detail.name,
            imageUrl = detail.imageUrl,
            types = detail.types.map {
                PokemonTypeEntity(pokemonId = detail.id, typeName = it.type.name, slot = it.slot)
            },
        )
    }

    override suspend fun ensureSpecies(id: Int): Boolean {
        if (pokemonDao.getPokemon(id)?.speciesFetched == true) return true
        return try {
            val species = api.getPokemonSpecies(id)
            pokemonDao.applySpecies(
                id = id,
                description = species.englishFlavorText(),
                evolvesFromId = species.evolvesFromSpecies?.id,
                evolvesFromName = species.evolvesFromSpecies?.name,
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val POKEMON_LIMIT = 151
        const val MAX_CONCURRENT_DETAIL_FETCHES = 5
    }
}
