package com.welsenho.pokedex.testing

import com.welsenho.pokedex.data.local.PokemonDao
import com.welsenho.pokedex.data.local.PokemonEntity
import com.welsenho.pokedex.data.local.PokemonTypeEntity
import com.welsenho.pokedex.data.local.TypeRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory PokemonDao mirroring the Room implementation's semantics. */
class FakePokemonDao : PokemonDao {

    val pokemon = MutableStateFlow<Map<Int, PokemonEntity>>(emptyMap())
    val types = MutableStateFlow<Map<Pair<Int, String>, PokemonTypeEntity>>(emptyMap())

    override suspend fun insertRoster(pokemon: List<PokemonEntity>) {
        // OnConflictStrategy.IGNORE: never clobber existing rows.
        this.pokemon.update { map -> map + pokemon.filter { it.id !in map }.associateBy { it.id } }
    }

    override suspend fun countRoster(limit: Int): Int =
        pokemon.value.keys.count { it <= limit }

    override suspend fun missingDetailIds(limit: Int): List<Int> =
        pokemon.value.values.filter { !it.detailFetched && it.id <= limit }.map { it.id }.sorted()

    override suspend fun updateDetail(id: Int, name: String, imageUrl: String?) {
        pokemon.update { map ->
            map[id]?.let {
                map + (id to it.copy(name = name, imageUrl = imageUrl, detailFetched = true))
            } ?: map
        }
    }

    override suspend fun insertTypes(types: List<PokemonTypeEntity>) {
        this.types.update { map -> map + types.associateBy { it.pokemonId to it.typeName } }
    }

    override suspend fun applySpecies(
        id: Int,
        description: String?,
        evolvesFromId: Int?,
        evolvesFromName: String?,
    ) {
        pokemon.update { map ->
            map[id]?.let {
                map + (id to it.copy(
                    speciesFetched = true,
                    description = description,
                    evolvesFromId = evolvesFromId,
                    evolvesFromName = evolvesFromName,
                ))
            } ?: map
        }
    }

    override fun observeTypeRows(limit: Int): Flow<List<TypeRow>> =
        combine(pokemon, types) { pokemonMap, typeMap ->
            typeMap.values
                .mapNotNull { t ->
                    pokemonMap[t.pokemonId]
                        ?.takeIf { it.detailFetched && it.id <= limit }
                        ?.let { TypeRow(t.typeName, it) }
                }
                .sortedWith(compareBy({ it.typeName }, { it.pokemon.id }))
        }

    override fun observePokemon(id: Int): Flow<PokemonEntity?> = pokemon.map { it[id] }

    override suspend fun getPokemon(id: Int): PokemonEntity? = pokemon.value[id]

    override fun observeTypesOf(id: Int): Flow<List<String>> =
        types.map { map -> map.values.filter { it.pokemonId == id }.sortedBy { it.slot }.map { it.typeName } }

    override suspend fun exists(id: Int): Boolean = id in pokemon.value
}
