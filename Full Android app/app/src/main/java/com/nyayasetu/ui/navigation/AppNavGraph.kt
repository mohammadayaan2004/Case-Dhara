package com.nyayasetu.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nyayasetu.ui.screens.bookmarks.BookmarkScreen
import com.nyayasetu.ui.screens.chatbot.ChatScreen
import com.nyayasetu.ui.screens.home.HomeScreen
import com.nyayasetu.ui.screens.mapper.CompareScreen
import com.nyayasetu.ui.screens.mapper.MapperScreen
import com.nyayasetu.ui.screens.mapper.SectionDetailScreen
import com.nyayasetu.ui.screens.summarizer.SummarizerScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME,
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(
            route = "${NavRoutes.MAPPER}?query={query}",
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            MapperScreen(navController = navController)
        }
        composable(
            route = NavRoutes.SECTION_DETAIL,
            arguments = listOf(
                navArgument("recordId") { type = NavType.IntType },
            ),
        ) {
            SectionDetailScreen(navController = navController)
        }
        composable(
            route = NavRoutes.COMPARE,
            arguments = listOf(
                navArgument("recordId") { type = NavType.IntType },
            ),
        ) {
            CompareScreen(navController = navController)
        }
        composable(NavRoutes.SUMMARIZER) {
            SummarizerScreen(navController = navController)
        }
        composable(NavRoutes.CHAT) {
            ChatScreen(navController = navController)
        }
        composable(NavRoutes.BOOKMARKS) {
            BookmarkScreen(navController = navController)
        }
    }
}
