package com.welsenho.pokedex.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class CaptureWithPokemon(
    @Embedded val capture: CaptureEntity,
    @Relation(parentColumn = "pokemonId", entityColumn = "id")
    val pokemon: PokemonEntity,
)

@Dao
interface CaptureDao {

    @Insert
    suspend fun insert(capture: CaptureEntity): Long

    @Query("DELETE FROM captures WHERE id = :captureId")
    suspend fun delete(captureId: Long)

    /** Most recent capture first; id breaks ties for same-millisecond captures. */
    @Transaction
    @Query("SELECT * FROM captures ORDER BY capturedAt DESC, id DESC")
    fun observeAll(): Flow<List<CaptureWithPokemon>>
}
