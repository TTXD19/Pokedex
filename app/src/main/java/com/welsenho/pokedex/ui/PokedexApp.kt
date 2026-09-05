package com.welsenho.pokedex.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.welsenho.pokedex.ui.detail.DetailScreen
import com.welsenho.pokedex.ui.detail.DetailViewModel
import com.welsenho.pokedex.ui.home.HomeScreen
import com.welsenho.pokedex.ui.home.HomeViewModel

@Composable
fun PokedexApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            HomeScreen(
                viewModel = viewModel,
                onPokemonClick = { id -> navController.navigate("detail/$id") },
            )
        }
        composable(
            route = "detail/{${DetailViewModel.ARG_POKEMON_ID}}",
            arguments = listOf(
                navArgument(DetailViewModel.ARG_POKEMON_ID) { type = NavType.IntType }
            ),
        ) {
            val viewModel: DetailViewModel = viewModel(factory = DetailViewModel.Factory)
            DetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                // Evolves-from pushes another detail entry so back walks up the chain.
                onNavigateToPokemon = { id -> navController.navigate("detail/$id") },
            )
        }
    }
}
