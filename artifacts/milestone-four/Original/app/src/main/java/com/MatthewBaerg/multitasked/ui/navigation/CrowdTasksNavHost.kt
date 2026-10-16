package com.MatthewBaerg.multitasked.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.MatthewBaerg.multitasked.ui.auth.AuthScreen
import com.MatthewBaerg.multitasked.ui.auth.AuthViewModel
import com.MatthewBaerg.multitasked.ui.boards.BoardsScreen
import com.MatthewBaerg.multitasked.ui.boards.BoardsViewModel
import com.MatthewBaerg.multitasked.ui.profile.ProfileScreen
import com.MatthewBaerg.multitasked.ui.profile.ProfileViewModel
import com.MatthewBaerg.multitasked.ui.tasks.BoardDetailScreen
import com.MatthewBaerg.multitasked.ui.tasks.BoardDetailViewModel
import com.MatthewBaerg.multitasked.ui.theme.AppTheme


/**
 * Central navigation host.
 * Wires up routes to composable screens and injects ViewModels via Hilt.
 */
@Composable
fun CrowdTasksNavHost(
    navController: NavHostController,
    startDestination: String,
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- Auth screen -----------------------------------------------------
        composable(Routes.AUTH) {
            val vm: AuthViewModel = hiltViewModel()
            AuthScreen(
                viewModel = vm,
                onAuthSuccess = {
                    navController.navigate(Routes.BOARDS) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // --- Boards list -----------------------------------------------------
        composable(Routes.BOARDS) {
            val vm: BoardsViewModel = hiltViewModel()
            BoardsScreen(
                viewModel = vm,
                onBoardSelected = { boardId ->
                    navController.navigate("${Routes.BOARD_DETAIL}/$boardId")
                },
                onLogout = {
                    // Go back to auth and clear back stack
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onProfile = { navController.navigate(Routes.PROFILE) },
                currentTheme = currentTheme,
                onThemeChange = onThemeChange
            )
        }

        // --- Board details / tasks ------------------------------------------
        composable("${Routes.BOARD_DETAIL}/{boardId}") { backStackEntry ->
            val boardId = backStackEntry.arguments?.getString("boardId") ?: return@composable
            val vm: BoardDetailViewModel = hiltViewModel()

            // ViewModel should already know boardId via SavedStateHandle,
            // we just navigate here.
            BoardDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // --- Profile --------------------------------------------------------
        composable(Routes.PROFILE) {
            val vm: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
