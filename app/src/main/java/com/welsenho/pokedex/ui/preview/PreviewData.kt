package com.welsenho.pokedex.ui.preview

import com.welsenho.pokedex.data.local.PokemonEntity
import com.welsenho.pokedex.data.repository.TypeGroup
import com.welsenho.pokedex.ui.home.HomeUiState

/** Sample data for @Preview composables only — never used at runtime. */
internal object PreviewData {

    val charmander = PokemonEntity(
        id = 4, name = "charmander", detailFetched = true, speciesFetched = true,
        description = "Obviously prefers hot places. When it rains, steam is said to spout from the tip of its tail.",
    )
    val charmeleon = PokemonEntity(
        id = 5, name = "charmeleon", detailFetched = true, speciesFetched = true,
        evolvesFromId = 4, evolvesFromName = "charmander",
    )
    val charizard = PokemonEntity(
        id = 6, name = "charizard", detailFetched = true, speciesFetched = true,
        description = "Spits fire that is hot enough to melt boulders. Known to cause forest fires unintentionally.",
        evolvesFromId = 5, evolvesFromName = "charmeleon",
    )
    val pikachu = PokemonEntity(
        id = 25, name = "pikachu", detailFetched = true, speciesFetched = true,
        description = "When several of these POKéMON gather, their electricity could build and cause lightning storms.",
        evolvesFromId = 172, evolvesFromName = "pichu",
    )
    val dratini = PokemonEntity(id = 147, name = "dratini", detailFetched = true)
    val dragonair = PokemonEntity(id = 148, name = "dragonair", detailFetched = true)
    val dragonite = PokemonEntity(id = 149, name = "dragonite", detailFetched = true)

    val typeGroups = listOf(
        TypeGroup("dragon", listOf(dratini, dragonair, dragonite)),
        TypeGroup("fire", listOf(charmander, charmeleon, charizard)),
    )

    val captured = listOf(
        HomeUiState.Companion.CapturedItem(
            captureId = 3,
            pokemonId = 147,
            name = "dratini",
            imageUrl = null
        ),
        HomeUiState.Companion.CapturedItem(
            captureId = 2,
            pokemonId = 25,
            name = "pikachu",
            imageUrl = null
        ),
        HomeUiState.Companion.CapturedItem(
            captureId = 1,
            pokemonId = 25,
            name = "pikachu",
            imageUrl = null
        ),
    )
}
