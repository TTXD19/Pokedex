package com.welsenho.pokedex.di

import android.util.Log
import com.welsenho.pokedex.BuildConfig
import com.welsenho.pokedex.data.local.PokedexDatabase
import com.welsenho.pokedex.data.network.ConnectivityNetworkMonitor
import com.welsenho.pokedex.data.network.NetworkMonitor
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

private const val MAX_LOG_LINE_CHARS = 4000

val dataModule = module {

    single {
        // PokeAPI payloads are huge; we only model the fields we use.
        Json { ignoreUnknownKeys = true }
    }

    single {
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    val logger = HttpLoggingInterceptor { message ->
                        val line =
                            if (message.length > MAX_LOG_LINE_CHARS) {
                                message.take(MAX_LOG_LINE_CHARS) +
                                    "… (truncated, ${message.length} chars total)"
                            } else {
                                message
                            }
                        Log.d("PokeApi", line)
                    }
                    addInterceptor(logger.setLevel(HttpLoggingInterceptor.Level.BODY))
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
    single<NetworkMonitor> { ConnectivityNetworkMonitor(androidContext()) }
}

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::DetailViewModel)
}
