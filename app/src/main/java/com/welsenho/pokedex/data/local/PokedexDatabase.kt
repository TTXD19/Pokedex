package com.welsenho.pokedex.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PokemonEntity::class, PokemonTypeEntity::class, CaptureEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PokedexDatabase : RoomDatabase() {

    abstract fun pokemonDao(): PokemonDao
    abstract fun captureDao(): CaptureDao

    companion object {
        fun create(context: Context): PokedexDatabase =
            Room.databaseBuilder(context, PokedexDatabase::class.java, "pokedex.db")
                .build()
    }
}
