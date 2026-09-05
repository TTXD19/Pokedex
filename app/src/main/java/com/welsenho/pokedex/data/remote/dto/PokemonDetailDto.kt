package com.welsenho.pokedex.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonDetailDto(
    val id: Int,
    val name: String,
    val sprites: SpritesDto,
    val types: List<TypeSlotDto>,
) {
    val imageUrl: String?
        get() = sprites.other?.officialArtwork?.frontDefault
}

@Serializable
data class SpritesDto(
    val other: OtherSpritesDto? = null,
)

@Serializable
data class OtherSpritesDto(
    @SerialName("official-artwork")
    val officialArtwork: OfficialArtworkDto? = null,
)

@Serializable
data class OfficialArtworkDto(
    @SerialName("front_default")
    val frontDefault: String? = null,
)

@Serializable
data class TypeSlotDto(
    val slot: Int,
    val type: NamedApiResource,
)
