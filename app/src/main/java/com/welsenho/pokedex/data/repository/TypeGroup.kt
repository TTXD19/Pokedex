package com.welsenho.pokedex.data.repository

import com.welsenho.pokedex.data.local.PokemonEntity

data class TypeGroup(
    val name: String,
    val pokemon: List<PokemonEntity>,
)
