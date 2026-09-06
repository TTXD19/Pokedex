package com.welsenho.pokedex.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Koin resolves the graph at runtime, so a missing binding crashes on first
 * use instead of failing the build. This test pulls that failure back into
 * the test suite by statically verifying the full module graph.
 */
class KoinModulesTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `koin module graph resolves`() {
        module { includes(dataModule, viewModelModule) }.verify(
            // Provided by the platform at runtime, not by our modules.
            extraTypes = listOf(Context::class, SavedStateHandle::class),
        )
    }
}
