package com.casedhara.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.casedhara.ui.components.design.dharaEnter
import com.casedhara.ui.components.design.dharaExit
import com.casedhara.ui.components.design.dharaPopEnter
import com.casedhara.ui.components.design.dharaPopExit
import com.casedhara.ui.screens.about.AboutScreen
import com.casedhara.ui.screens.auth.LoginScreen
import com.casedhara.ui.screens.auth.SignupScreen
import com.casedhara.ui.screens.bookmarks.BookmarkScreen
import com.casedhara.ui.screens.chatbot.ChatScreen
import com.casedhara.ui.screens.home.HomeScreen
import com.casedhara.ui.screens.mapper.CompareScreen
import com.casedhara.ui.screens.mapper.MapperScreen
import com.casedhara.ui.screens.mapper.SectionDetailScreen
import com.casedhara.ui.screens.quiz.QuizScreen
import com.casedhara.ui.screens.settings.CaseSummaryDetailScreen
import com.casedhara.ui.screens.settings.ProfileScreen
import com.casedhara.ui.screens.settings.QuizProgressDetailScreen
import com.casedhara.ui.screens.settings.QuizProgressScreen
import com.casedhara.ui.screens.settings.SavedCaseSummariesScreen
import com.casedhara.ui.screens.settings.SettingsScreen
import com.casedhara.ui.screens.settings.WrongAnswerDetailScreen
import com.casedhara.ui.screens.settings.WrongAnswersScreen
import com.casedhara.ui.screens.splash.SplashScreen
import com.casedhara.ui.screens.summarizer.SummarizerScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH,
        enterTransition = { dharaEnter() },
        exitTransition = { dharaExit() },
        popEnterTransition = { dharaPopEnter() },
        popExitTransition = { dharaPopExit() },
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(navController = navController)
        }
        composable(NavRoutes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(NavRoutes.MAPPER) {
            MapperScreen(navController = navController)
        }
        composable(
            route = "${NavRoutes.MAPPER}?query={query}&openDetail={openDetail}",
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("openDetail") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) {
            MapperScreen(navController = navController)
        }
        composable(
            route = NavRoutes.SECTION_DETAIL,
            arguments = listOf(navArgument("recordId") { type = NavType.IntType }),
        ) {
            SectionDetailScreen(navController = navController)
        }
        composable(
            route = NavRoutes.COMPARE,
            arguments = listOf(navArgument("recordId") { type = NavType.IntType }),
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
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(NavRoutes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(NavRoutes.WRONG_ANSWERS) {
            WrongAnswersScreen(navController = navController)
        }
        composable(NavRoutes.QUIZ_PROGRESS) {
            QuizProgressScreen(navController = navController)
        }
        composable(NavRoutes.SAVED_CASE_SUMMARIES) {
            SavedCaseSummariesScreen(navController = navController)
        }
        composable(
            route = "${NavRoutes.CASE_SUMMARY_DETAIL}/{caseId}",
            arguments = listOf(navArgument("caseId") { type = NavType.StringType }),
        ) { backStack ->
            val id = backStack.arguments?.getString("caseId") ?: return@composable
            CaseSummaryDetailScreen(navController = navController, caseId = id)
        }
        composable(
            route = "${NavRoutes.WRONG_ANSWER_DETAIL}/{answerId}",
            arguments = listOf(navArgument("answerId") { type = NavType.StringType }),
        ) { backStack ->
            val id = backStack.arguments?.getString("answerId") ?: return@composable
            WrongAnswerDetailScreen(navController = navController, answerId = id)
        }
        composable(
            route = "${NavRoutes.QUIZ_PROGRESS_DETAIL}/{progressId}",
            arguments = listOf(navArgument("progressId") { type = NavType.StringType }),
        ) { backStack ->
            val id = backStack.arguments?.getString("progressId") ?: return@composable
            QuizProgressDetailScreen(navController = navController, progressId = id)
        }
        composable(NavRoutes.ABOUT) {
            AboutScreen(navController = navController)
        }
        composable(NavRoutes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(NavRoutes.SIGNUP) {
            SignupScreen(navController = navController)
        }
        composable(NavRoutes.QUIZ) {
            QuizScreen(navController = navController)
        }
    }
}
