package com.welsenho.pokedex

import android.app.Application
import com.welsenho.pokedex.di.dataModule
import com.welsenho.pokedex.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PokedexApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PokedexApplication)
            modules(dataModule, viewModelModule)
        }
    }
}
