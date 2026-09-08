package com.welsenho.pokedex.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.welsenho.pokedex.ui.detail.DetailScreen
import com.welsenho.pokedex.ui.detail.DetailViewModel
import com.welsenho.pokedex.ui.home.HomeScreen
import com.welsenho.pokedex.ui.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PokedexApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val viewModel: HomeViewModel = koinViewModel()
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
            val viewModel: DetailViewModel = koinViewModel()
            DetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPokemon = { id -> navController.navigate("detail/$id") },
            )
        }
    }
}
