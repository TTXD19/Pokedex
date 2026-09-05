package com.welsenho.pokedex.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per Pokémon. Rows are created from the roster fetch (id + name only);
 * detail and species fields are filled in later. The two *Fetched flags are what
 * makes sync resumable: after process death we re-fetch only rows still false.
 */
@Entity(tableName = "pokemon")
data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val imageUrl: String? = null,
    val detailFetched: Boolean = false,
    // Species (detail screen) data, fetched lazily on first open.
    val speciesFetched: Boolean = false,
    val description: String? = null,
    val evolvesFromId: Int? = null,
    val evolvesFromName: String? = null,
)

/**
 * Pokémon ↔ type is many-to-many. Types are plain names, not a table of their
 * own — PokeAPI's type resource has nothing else we need.
 */
@Entity(
    tableName = "pokemon_types",
    primaryKeys = ["pokemonId", "typeName"],
    foreignKeys = [
        ForeignKey(
            entity = PokemonEntity::class,
            parentColumns = ["id"],
            childColumns = ["pokemonId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("typeName")],
)
data class PokemonTypeEntity(
    val pokemonId: Int,
    val typeName: String,
    val slot: Int,
)

/**
 * A capture is an event, not a flag on the Pokémon: the same species can be
 * captured many times, each row released individually (requirement 1).
 */
@Entity(
    tableName = "captures",
    foreignKeys = [
        ForeignKey(
            entity = PokemonEntity::class,
            parentColumns = ["id"],
            childColumns = ["pokemonId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("pokemonId")],
)
data class CaptureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pokemonId: Int,
    val capturedAt: Long,
)
