package com.welsenho.pokedex.ui.home

import app.cash.turbine.test
import com.welsenho.pokedex.data.local.PokemonEntity
import com.welsenho.pokedex.data.repository.SyncState
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
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePokemonRepository
    private lateinit var networkMonitor: FakeNetworkMonitor

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakePokemonRepository()
        networkMonitor = FakeNetworkMonitor()
        repository.addPokemon(PokemonEntity(id = 25, name = "pikachu", imageUrl = "url25", detailFetched = true))
    }

    private fun viewModel() = HomeViewModel(repository, networkMonitor)

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts a sync on creation`() = runTest(dispatcher.scheduler) {
        val viewModel = viewModel()
        dispatcher.scheduler.runCurrent()

        assertEquals(1, repository.syncCalls)
        viewModel.uiState.test { cancelAndIgnoreRemainingEvents() }
    }

    @Test
    fun `capturing twice shows two entries, releasing one keeps the other`() =
        runTest(dispatcher.scheduler) {
            val viewModel = viewModel()

            viewModel.uiState.test {
                viewModel.capture(25)
                viewModel.capture(25)

                var state = awaitItemWhere { it.captured.size == 2 }
                // Most recent capture first: the second capture has the later timestamp.
                assertEquals(listOf(2L, 1L), state.captured.map { it.captureId })
                assertEquals(listOf("pikachu", "pikachu"), state.captured.map { it.name })

                viewModel.release(state.captured.first().captureId)

                state = awaitItemWhere { it.captured.size == 1 }
                // The specific released capture is gone; the other survives.
                assertEquals(listOf(1L), state.captured.map { it.captureId })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `full screen error only when roster unavailable and nothing to show`() =
        runTest(dispatcher.scheduler) {
            val viewModel = viewModel()

            viewModel.uiState.test {
                repository.setSyncState(SyncState.Failed(failedDetails = 0, rosterUnavailable = true))
                val errorState = awaitItemWhere { it.syncState is SyncState.Failed }
                assertTrue(errorState.showFullScreenError)

                // Same failure kind but content exists -> banner, not full-screen error.
                repository.typeGroups.value = listOf(
                    com.welsenho.pokedex.data.repository.TypeGroup(
                        "electric",
                        listOf(repository.pokemonById.value.getValue(25)),
                    )
                )
                val partialState = awaitItemWhere { it.typeGroups.isNotEmpty() }
                assertFalse(partialState.showFullScreenError)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `offline state reaches the ui`() = runTest(dispatcher.scheduler) {
        val viewModel = viewModel()

        viewModel.uiState.test {
            networkMonitor.online.value = false
            val offline = awaitItemWhere { !it.isOnline }
            assertFalse(offline.isOnline)

            networkMonitor.online.value = true
            val online = awaitItemWhere { it.isOnline }
            assertTrue(online.isOnline)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `regaining connectivity triggers a sync, going offline does not`() =
        runTest(dispatcher.scheduler) {
            val viewModel = viewModel()
            dispatcher.scheduler.runCurrent()
            assertEquals(1, repository.syncCalls) // init only

            networkMonitor.online.value = false
            dispatcher.scheduler.runCurrent()
            assertEquals(1, repository.syncCalls)

            networkMonitor.online.value = true
            dispatcher.scheduler.runCurrent()
            assertEquals(2, repository.syncCalls)
            viewModel.uiState.test { cancelAndIgnoreRemainingEvents() }
        }

    private suspend fun app.cash.turbine.TurbineTestContext<HomeUiState>.awaitItemWhere(
        predicate: (HomeUiState) -> Boolean,
    ): HomeUiState {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }
}
