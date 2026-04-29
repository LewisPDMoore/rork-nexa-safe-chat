package com.rork.nexa.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rork.nexa.data.AppState
import com.rork.nexa.ui.screens.ChatDetailScreen
import com.rork.nexa.ui.screens.OnboardingScreen
import com.rork.nexa.ui.screens.ParentDashboardScreen
import com.rork.nexa.ui.screens.RootScaffold

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val start = if (AppState.hasOnboarded) "root" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = start,
        enterTransition = { fadeIn(tween(200)) },
        exitTransition = { fadeOut(tween(200)) },
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onDone = {
                    navController.navigate("root") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("root") {
            RootScaffold(navController = navController)
        }
        composable(
            route = "chat/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(220)) },
            exitTransition = { slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(220)) },
        ) { entry ->
            val chatId = entry.arguments?.getString("chatId") ?: return@composable
            ChatDetailScreen(chatId = chatId, navController = navController)
        }
        composable(
            route = "parent",
            enterTransition = { slideInHorizontally(tween(240)) { it / 2 } + fadeIn(tween(240)) },
            exitTransition = { slideOutHorizontally(tween(240)) { it / 2 } + fadeOut(tween(240)) },
        ) {
            ParentDashboardScreen(navController = navController)
        }
    }
}
