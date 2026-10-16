package com.MatthewBaerg.multitasked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.MatthewBaerg.multitasked.data.repository.AuthRepository
import com.MatthewBaerg.multitasked.ui.SettingsRepository
import com.MatthewBaerg.multitasked.ui.navigation.CrowdTasksNavHost
import com.MatthewBaerg.multitasked.ui.navigation.Routes
import com.MatthewBaerg.multitasked.ui.theme.AppTheme
import com.MatthewBaerg.multitasked.ui.theme.MultiTaskedTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val Authenticating = "authenticating"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            MultiTaskedApp(
                authRepository = authRepository,
                settingsRepository = settingsRepository
            )
        }
    }
}

@Composable
fun MultiTaskedApp(
    authRepository: AuthRepository,
    settingsRepository: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    val currentTheme by settingsRepository.theme.collectAsState(initial = AppTheme.SYSTEM)

    MultiTaskedTheme(theme = currentTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()

            val userId by authRepository.authStateChanges().collectAsState(initial = Authenticating)

            if (userId == Authenticating) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val startDestination = if (userId != null) {
                    Routes.BOARDS
                } else {
                    Routes.AUTH
                }

                CrowdTasksNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    currentTheme = currentTheme,
                    onThemeChange = { newTheme: AppTheme ->
                        scope.launch {
                            settingsRepository.setTheme(newTheme)
                        }
                    }
                )
            }
        }
    }
}
