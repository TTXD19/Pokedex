package com.welsenho.pokedex.di

import com.welsenho.pokedex.BuildConfig
import com.welsenho.pokedex.data.local.PokedexDatabase
import com.welsenho.pokedex.data.remote.PokeApiService
import com.welsenho.pokedex.data.repository.PokemonRepository
import com.welsenho.pokedex.data.repository.PokemonRepositoryImpl
import com.welsenho.pokedex.ui.detail.DetailViewModel
import com.welsenho.pokedex.ui.home.HomeViewModel
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val dataModule = module {

    single {
        // PokeAPI payloads are huge; we only model the fields we use.
        Json { ignoreUnknownKeys = true }
    }

    single {
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
                    )
                }
            }
            .build()
    }

    single<PokeApiService> {
        Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .client(get())
            .addConverterFactory(get<Json>().asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PokeApiService::class.java)
    }

    single { PokedexDatabase.create(androidContext()) }
    single { get<PokedexDatabase>().pokemonDao() }
    single { get<PokedexDatabase>().captureDao() }

    single<PokemonRepository> { PokemonRepositoryImpl(get(), get(), get()) }
}

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    // SavedStateHandle (nav args) is injected by Koin's ViewModel factory.
    viewModelOf(::DetailViewModel)
}
