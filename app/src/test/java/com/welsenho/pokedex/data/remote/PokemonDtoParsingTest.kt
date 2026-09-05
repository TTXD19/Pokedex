package com.welsenho.pokedex.data.remote

import com.welsenho.pokedex.data.remote.dto.NamedApiResource
import com.welsenho.pokedex.data.remote.dto.PokemonDetailDto
import com.welsenho.pokedex.data.remote.dto.PokemonSpeciesDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Parsing tests against trimmed-down but structurally real PokeAPI payloads.
 * The risky spots: the hyphenated "official-artwork" key, snake_case fields,
 * and nulls (missing artwork, no pre-evolution).
 */
class PokemonDtoParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `detail parses artwork url and types from nested sprites`() {
        val payload = """
            {
              "id": 6,
              "name": "charizard",
              "base_experience": 267,
              "sprites": {
                "front_default": "https://raw.githubusercontent.com/sprites/6.png",
                "other": {
                  "dream_world": { "front_default": null },
                  "official-artwork": {
                    "front_default": "https://raw.githubusercontent.com/official/6.png",
                    "front_shiny": "https://raw.githubusercontent.com/official/shiny/6.png"
                  }
                }
              },
              "types": [
                { "slot": 1, "type": { "name": "fire", "url": "https://pokeapi.co/api/v2/type/10/" } },
                { "slot": 2, "type": { "name": "flying", "url": "https://pokeapi.co/api/v2/type/3/" } }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<PokemonDetailDto>(payload)

        assertEquals(6, dto.id)
        assertEquals("https://raw.githubusercontent.com/official/6.png", dto.imageUrl)
        assertEquals(listOf("fire", "flying"), dto.types.map { it.type.name })
    }

    @Test
    fun `detail with missing artwork yields null imageUrl instead of crashing`() {
        val payload = """
            {
              "id": 999,
              "name": "missingno",
              "sprites": { "other": {} },
              "types": []
            }
        """.trimIndent()

        val dto = json.decodeFromString<PokemonDetailDto>(payload)

        assertNull(dto.imageUrl)
    }

    @Test
    fun `species picks english flavor text and collapses control characters`() {
        val payload = """
            {
              "id": 6,
              "name": "charizard",
              "evolves_from_species": { "name": "charmeleon", "url": "https://pokeapi.co/api/v2/pokemon-species/5/" },
              "flavor_text_entries": [
                {
                  "flavor_text": "Il crache un feu.",
                  "language": { "name": "fr", "url": "https://pokeapi.co/api/v2/language/5/" },
                  "version": { "name": "red", "url": "https://pokeapi.co/api/v2/version/1/" }
                },
                {
                  "flavor_text": "Spits fire that\nis hot enough to\fmelt boulders.",
                  "language": { "name": "en", "url": "https://pokeapi.co/api/v2/language/9/" },
                  "version": { "name": "red", "url": "https://pokeapi.co/api/v2/version/1/" }
                }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<PokemonSpeciesDto>(payload)

        assertEquals("Spits fire that is hot enough to melt boulders.", dto.englishFlavorText())
        assertEquals(5, dto.evolvesFromSpecies?.id)
    }

    @Test
    fun `species without pre-evolution parses evolves_from as null`() {
        val payload = """
            { "id": 4, "name": "charmander", "evolves_from_species": null, "flavor_text_entries": [] }
        """.trimIndent()

        val dto = json.decodeFromString<PokemonSpeciesDto>(payload)

        assertNull(dto.evolvesFromSpecies)
        assertNull(dto.englishFlavorText())
    }

    @Test
    fun `resource id is parsed from trailing url segment`() {
        assertEquals(151, NamedApiResource("mew", "https://pokeapi.co/api/v2/pokemon/151/").id)
        assertEquals(1, NamedApiResource("bulbasaur", "https://pokeapi.co/api/v2/pokemon/1").id)
    }
}
