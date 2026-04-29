package com.rork.nexa.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rork.nexa.data.AppState
import com.rork.nexa.data.IntroPrefs
import com.rork.nexa.data.auth.SessionStatus
import com.rork.nexa.ui.screens.AdminDashboardScreen
import com.rork.nexa.ui.screens.BanScreen
import com.rork.nexa.ui.screens.ChatDetailScreen
import com.rork.nexa.ui.screens.FamilyCenterScreen
import com.rork.nexa.ui.screens.OnboardingScreen
import com.rork.nexa.ui.screens.ParentDashboardScreen
import com.rork.nexa.ui.screens.ReportStatusScreen
import com.rork.nexa.ui.screens.RootScaffold
import com.rork.nexa.ui.screens.SplashScreen
import com.rork.nexa.ui.screens.VibePickScreen
import com.rork.nexa.ui.screens.auth.LoginScreen
import com.rork.nexa.ui.screens.auth.SignUpScreen
import com.rork.nexa.viewmodels.AuthViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val status by authViewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(status) {
        when (val s = status) {
            is SessionStatus.Authenticated -> {
                s.profile?.let { p ->
                    AppState.applyProfile(
                        username = p.username,
                        avatarEmoji = p.avatarEmoji,
                        avatarGradientIndex = p.avatarGradient,
                    )
                }
                val current = navController.currentBackStackEntry?.destination?.route
                if (current == null || current == "splash" || current == "login" || current == "onboarding" || current == "banned") {
                    navController.navigate("root") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is SessionStatus.Banned -> {
                AppState.clearUserData()
                val current = navController.currentBackStackEntry?.destination?.route
                if (current != "banned") {
                    navController.navigate("banned") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            SessionStatus.Unauthenticated -> {
                AppState.clearUserData()
                val current = navController.currentBackStackEntry?.destination?.route
                if (current == null || current == "splash" || current == "root" || current == "vibe" || current == "banned") {
                    val target = if (IntroPrefs.hasSeen(context)) "login" else "onboarding"
                    navController.navigate(target) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            SessionStatus.Loading -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = { fadeIn(tween(200)) },
        exitTransition = { fadeOut(tween(200)) },
    ) {
        composable("splash") { SplashScreen() }
        composable("onboarding") {
            OnboardingScreen(
                onSignUp = {
                    IntroPrefs.markSeen(context)
                    navController.navigate("signup")
                },
                onLogin = {
                    IntroPrefs.markSeen(context)
                    navController.navigate("login")
                },
            )
        }
        composable("signup") {
            SignUpScreen(
                onSignedUp = {
                    IntroPrefs.markSeen(context)
                    navController.navigate("vibe") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onGoToLogin = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                viewModel = authViewModel,
            )
        }
        composable("login") {
            LoginScreen(
                onLoggedIn = {
                    IntroPrefs.markSeen(context)
                    navController.navigate("root") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onGoToSignUp = {
                    navController.navigate("signup") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                viewModel = authViewModel,
            )
        }
        composable("vibe") {
            VibePickScreen(
                onDone = {
                    navController.navigate("root") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                viewModel = authViewModel,
            )
        }
        composable("banned") {
            val s = status as? SessionStatus.Banned
            BanScreen(
                until = s?.until,
                reason = s?.reason,
                onContinue = { authViewModel.acknowledgeBan() },
            )
        }
        composable("root") { RootScaffold(navController = navController) }
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
        composable(
            route = "admin",
            enterTransition = { slideInHorizontally(tween(240)) { it / 2 } + fadeIn(tween(240)) },
            exitTransition = { slideOutHorizontally(tween(240)) { it / 2 } + fadeOut(tween(240)) },
        ) {
            val s = status
            if (s is SessionStatus.Authenticated && s.profile?.isAdmin == true) {
                AdminDashboardScreen(navController = navController)
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
        composable(
            route = "family",
            enterTransition = { slideInHorizontally(tween(240)) { it / 2 } + fadeIn(tween(240)) },
            exitTransition = { slideOutHorizontally(tween(240)) { it / 2 } + fadeOut(tween(240)) },
        ) {
            val s = status
            val isChild = (s as? SessionStatus.Authenticated)?.profile?.parentId?.isNotBlank() == true
            if (isChild) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                FamilyCenterScreen(navController = navController)
            }
        }
        composable(
            route = "reports",
            enterTransition = { slideInHorizontally(tween(240)) { it / 2 } + fadeIn(tween(240)) },
            exitTransition = { slideOutHorizontally(tween(240)) { it / 2 } + fadeOut(tween(240)) },
        ) {
            ReportStatusScreen(navController = navController)
        }
    }
}
