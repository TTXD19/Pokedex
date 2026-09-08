package com.welsenho.pokedex.ui.detail

import com.welsenho.pokedex.data.local.PokemonEntity

data class DetailUiState(
    val pokemon: PokemonEntity? = null,
    val types: List<String> = emptyList(),
    val detailError: Boolean = false,
    val speciesError: Boolean = false,
    val evolvesFromTappable: Boolean = false,
    val evolvesFromImageUrl: String? = null,
    val isOnline: Boolean = true,
)
