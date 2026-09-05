package com.welsenho.pokedex.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NamedApiResource(
    val name: String,
    val url: String,
) {
    /**
     * PokeAPI list endpoints don't include ids; they are only present in the
     * resource url, e.g. "https://pokeapi.co/api/v2/pokemon/151/".
     */
    val id: Int
        get() = url.trimEnd('/').substringAfterLast('/').toInt()
}
