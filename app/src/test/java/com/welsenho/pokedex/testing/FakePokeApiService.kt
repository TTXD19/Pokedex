package com.welsenho.pokedex.testing

import com.welsenho.pokedex.data.remote.PokeApiService
import com.welsenho.pokedex.data.remote.dto.NamedApiResource
import com.welsenho.pokedex.data.remote.dto.OfficialArtworkDto
import com.welsenho.pokedex.data.remote.dto.OtherSpritesDto
import com.welsenho.pokedex.data.remote.dto.PokemonDetailDto
import com.welsenho.pokedex.data.remote.dto.PokemonListResponse
import com.welsenho.pokedex.data.remote.dto.PokemonSpeciesDto
import com.welsenho.pokedex.data.remote.dto.SpritesDto
import com.welsenho.pokedex.data.remote.dto.TypeSlotDto
import java.io.IOException
import kotlinx.coroutines.delay

/**
 * Programmable PokeApiService: choose which calls fail, record every call,
 * and track how many detail fetches were ever in flight at once.
 */
class FakePokeApiService : PokeApiService {

    var rosterFails = false
    var failingDetailIds: Set<Int> = emptySet()

    var rosterCalls = 0
        private set
    val detailCalls = mutableListOf<Int>()

    private var activeDetailCalls = 0
    var maxConcurrentDetailCalls = 0
        private set

    override suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponse {
        rosterCalls++
        if (rosterFails) throw IOException("no network")
        return PokemonListResponse(
            count = limit,
            results = (1..limit).map {
                NamedApiResource("pokemon-$it", "https://pokeapi.co/api/v2/pokemon/$it/")
            },
        )
    }

    override suspend fun getPokemonDetail(id: Int): PokemonDetailDto {
        detailCalls += id
        activeDetailCalls++
        maxConcurrentDetailCalls = maxOf(maxConcurrentDetailCalls, activeDetailCalls)
        delay(1) // suspension point so concurrent fetches actually overlap
        activeDetailCalls--
        if (id in failingDetailIds) throw IOException("failed fetching $id")
        return PokemonDetailDto(
            id = id,
            name = "pokemon-$id",
            sprites = SpritesDto(OtherSpritesDto(OfficialArtworkDto("https://img/$id.png"))),
            types = listOf(
                TypeSlotDto(slot = 1, type = NamedApiResource("normal", "https://pokeapi.co/api/v2/type/1/"))
            ),
        )
    }

    override suspend fun getPokemonSpecies(id: Int): PokemonSpeciesDto =
        throw UnsupportedOperationException("sync tests must not fetch species")
}
