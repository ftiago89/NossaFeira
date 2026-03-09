package com.example.nossafeira.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nossafeira.ui.screens.itens.ItensScreen
import com.example.nossafeira.ui.screens.listas.ListasScreen

object Destinos {
    const val LISTAS = "listas"
    const val ITENS = "itens/{listaId}"
    fun itens(listaId: Int) = "itens/$listaId"
}

@Composable
fun NossaFeiraNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Destinos.LISTAS
    ) {
        composable(Destinos.LISTAS) {
            ListasScreen(
                onListaClick = { listaId ->
                    navController.navigate(Destinos.itens(listaId))
                }
            )
        }

        composable(
            route = Destinos.ITENS,
            arguments = listOf(navArgument("listaId") { type = NavType.IntType })
        ) { backStackEntry ->
            val listaId = backStackEntry.arguments?.getInt("listaId") ?: return@composable
            ItensScreen(
                listaId = listaId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
