package com.welsenho.pokedex.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class TypeRow(
    val typeName: String,
    @Embedded val pokemon: PokemonEntity,
)

@Dao
interface PokemonDao {

    /** List insert must not clobber rows that already carry fetched detail. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPokemonList(pokemon: List<PokemonEntity>)

    /**
     * Rows with id > limit are cached on demand for the detail screen (e.g.
     * pre-evolutions like Igglybuff #174) and must never count toward the 151,
     * which is why the queries below scope to the id range: the assignment's
     * list is exactly ids 1..151.
     */
    @Query("SELECT COUNT(*) FROM pokemon WHERE id <= :limit")
    suspend fun getPokemonListSize(limit: Int): Int

    @Query("SELECT id FROM pokemon WHERE detailFetched = 0 AND id <= :limit ORDER BY id")
    suspend fun getMissingDetailIdsList(limit: Int): List<Int>

    @Query(
        "UPDATE pokemon SET name = :name, imageUrl = :imageUrl, detailFetched = 1 WHERE id = :id"
    )
    suspend fun updateDetail(id: Int, name: String, imageUrl: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTypes(types: List<PokemonTypeEntity>)

    @Transaction
    suspend fun applyDetail(id: Int, name: String, imageUrl: String?, types: List<PokemonTypeEntity>) {
        updateDetail(id, name, imageUrl)
        insertTypes(types)
    }

    @Query(
        """
        UPDATE pokemon
        SET speciesFetched = 1, description = :description,
            evolvesFromId = :evolvesFromId, evolvesFromName = :evolvesFromName
        WHERE id = :id
        """
    )
    suspend fun applySpecies(id: Int, description: String?, evolvesFromId: Int?, evolvesFromName: String?)

    /**
     * Backbone of the collection screen: categories alphabetical, Pokémon
     * within a category by id (ordering per the reference mock).
     */
    @Query(
        """
        SELECT pt.typeName AS typeName, p.*
        FROM pokemon_types pt JOIN pokemon p ON p.id = pt.pokemonId
        WHERE p.detailFetched = 1 AND p.id <= :limit
        ORDER BY pt.typeName ASC, p.id ASC
        """
    )
    fun observeTypeRows(limit: Int): Flow<List<TypeRow>>

    @Query("SELECT * FROM pokemon WHERE id = :id")
    fun observePokemon(id: Int): Flow<PokemonEntity?>

    @Query("SELECT * FROM pokemon WHERE id = :id")
    suspend fun getPokemon(id: Int): PokemonEntity?

    @Query("SELECT typeName FROM pokemon_types WHERE pokemonId = :id ORDER BY slot")
    fun observeTypesOf(id: Int): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM pokemon WHERE id = :id)")
    suspend fun exists(id: Int): Boolean
}
