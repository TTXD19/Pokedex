package com.welsenho.pokedex.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonSpeciesDto(
    val id: Int,
    val name: String,
    @SerialName("evolves_from_species")
    val evolvesFromSpecies: NamedApiResource? = null,
    @SerialName("flavor_text_entries")
    val flavorTextEntries: List<FlavorTextEntryDto> = emptyList(),
) {
    /**
     * Picks one English description. Entries repeat per game version with raw
     * control characters (\n, \f) that are line-wrapping hints for the Game Boy
     * screen, not content — collapse them to spaces.
     */
    fun englishFlavorText(): String? =
        flavorTextEntries.firstOrNull { it.language.name == "en" }
            ?.flavorText
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
}

@Serializable
data class FlavorTextEntryDto(
    @SerialName("flavor_text")
    val flavorText: String,
    val language: NamedApiResource,
    val version: NamedApiResource? = null,
)
