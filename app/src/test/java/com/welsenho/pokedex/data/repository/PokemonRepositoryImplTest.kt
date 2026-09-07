package com.welsenho.pokedex.data.repository

import com.welsenho.pokedex.data.local.PokemonEntity
import com.welsenho.pokedex.testing.FakeCaptureDao
import com.welsenho.pokedex.testing.FakePokeApiService
import com.welsenho.pokedex.testing.FakePokemonDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests the real sync algorithm against the assignment's promises:
 * "Minimize duplicate requests when fetching data, and it can be continued
 * after an unexpected interruption."
 */
class PokemonRepositoryImplTest {

    private lateinit var dao: FakePokemonDao
    private lateinit var api: FakePokeApiService

    @Before
    fun setUp() {
        dao = FakePokemonDao()
        api = FakePokeApiService()
    }

    private fun repository() = PokemonRepositoryImpl(
        pokemonDao = dao,
        captureDao = FakeCaptureDao(),
        api = api,
        clock = { 0L },
    )

    /** Simulates a previous run that died after fetching [fetchedUpTo] details. */
    private suspend fun seedInterruptedSync(fetchedUpTo: Int) {
        dao.insertPokemonList((1..151).map { PokemonEntity(id = it, name = "pokemon-$it") })
        (1..fetchedUpTo).forEach { dao.updateDetail(it, "pokemon-$it", "https://img/$it.png") }
    }

    @Test
    fun `interrupted sync resumes fetching only what is missing`() = runTest {
        seedInterruptedSync(fetchedUpTo = 100)
        val repository = repository()

        repository.sync()

        assertEquals(0, api.pokemonListCalls) // list already complete, not re-fetched
        assertEquals((101..151).toList(), api.detailCalls.sorted())
        assertEquals(emptyList<Int>(), dao.missingDetailIds(151))
        assertEquals(SyncState.Complete, repository.syncState.value)
    }

    @Test
    fun `fully synced data makes sync a zero-request no-op`() = runTest {
        seedInterruptedSync(fetchedUpTo = 151)
        val repository = repository()

        repository.sync()

        assertEquals(0, api.pokemonListCalls)
        assertEquals(emptyList<Int>(), api.detailCalls)
        assertEquals(SyncState.Complete, repository.syncState.value)
    }

    @Test
    fun `partial failures are isolated and counted`() = runTest {
        val failing = setOf(3, 20, 77, 90, 101, 140, 151)
        api.failingDetailIds = failing
        val repository = repository()

        repository.sync()

        assertEquals(
            SyncState.Failed(failedDetails = 7, pokemonListUnavailable = false),
            repository.syncState.value,
        )
        // Only the failed ids are still missing; successes all landed.
        assertEquals(failing.sorted(), dao.missingDetailIds(151))
        assertTrue(dao.getPokemon(1)!!.detailFetched)
    }

    @Test
    fun `retry after partial failure fetches only the failed ids`() = runTest {
        val failing = setOf(3, 20, 77, 90, 101, 140, 151)
        api.failingDetailIds = failing
        val repository = repository()
        repository.sync()

        api.failingDetailIds = emptySet()
        api.detailCalls.clear()
        repository.sync()

        assertEquals(failing.sorted(), api.detailCalls.sorted())
        assertEquals(SyncState.Complete, repository.syncState.value)
    }

    @Test
    fun `first launch offline reports the Pokémon list as unavailable`() = runTest {
        api.pokemonListFails = true
        val repository = repository()

        repository.sync()

        assertEquals(
            SyncState.Failed(failedDetails = 0, pokemonListUnavailable = true),
            repository.syncState.value,
        )
        assertEquals(emptyList<Int>(), api.detailCalls) // gave up before details
    }

    @Test
    fun `detail fetches never exceed the concurrency cap`() = runTest {
        val repository = repository()

        repository.sync()

        assertEquals(151, api.detailCalls.size)
        assertEquals(
            PokemonRepositoryImpl.MAX_CONCURRENT_DETAIL_FETCHES,
            api.maxConcurrentDetailCalls,
        )
    }
}
