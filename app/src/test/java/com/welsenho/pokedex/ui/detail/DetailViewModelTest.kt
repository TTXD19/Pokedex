package com.welsenho.pokedex.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.welsenho.pokedex.data.local.PokemonEntity
import com.welsenho.pokedex.testing.FakeNetworkMonitor
import com.welsenho.pokedex.testing.FakePokemonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePokemonRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakePokemonRepository()
        networkMonitor = FakeNetworkMonitor()
        repository.addPokemon(
            PokemonEntity(id = 25, name = "pikachu", imageUrl = "url25", detailFetched = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = DetailViewModel(
        repository = repository,
        networkMonitor = networkMonitor,
        savedStateHandle = SavedStateHandle(mapOf(DetailViewModel.ARG_POKEMON_ID to 25)),
    )

    @Test
    fun `regaining connectivity retries a failed species load`() =
        runTest(dispatcher.scheduler) {
            repository.ensureSpeciesResult = false // offline: species fetch fails
            val viewModel = viewModel()

            viewModel.uiState.test {
                val failed = awaitItemWhere { it.speciesError }
                assertTrue(failed.speciesError)
                assertEquals(1, repository.ensureSpeciesCalls)

                repository.ensureSpeciesResult = true // network back: fetch succeeds
                networkMonitor.online.value = false
                // Let the collector observe the offline value; StateFlow would
                // otherwise conflate false->true into no change at all.
                dispatcher.scheduler.runCurrent()
                networkMonitor.online.value = true

                val recovered = awaitItemWhere { !it.speciesError }
                assertFalse(recovered.speciesError)
                assertEquals(2, repository.ensureSpeciesCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `going offline alone does not retry`() = runTest(dispatcher.scheduler) {
        val viewModel = viewModel()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repository.ensureSpeciesCalls)

        networkMonitor.online.value = false
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.ensureSpeciesCalls)
        viewModel.uiState.test { cancelAndIgnoreRemainingEvents() }
    }

    private suspend fun app.cash.turbine.TurbineTestContext<DetailUiState>.awaitItemWhere(
        predicate: (DetailUiState) -> Boolean,
    ): DetailUiState {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }
}
