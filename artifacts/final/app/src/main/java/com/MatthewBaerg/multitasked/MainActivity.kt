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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.MatthewBaerg.multitasked.ui.local.LocalWorkspaceRoute
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
    var workspace by rememberSaveable { mutableStateOf<String?>(null) }

    MultiTaskedTheme(theme = currentTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (workspace == null) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
                    verticalArrangement = Arrangement.Center) {
                    Text("MultiTasked", style = MaterialTheme.typography.headlineLarge)
                    Text("Choose where to keep your boards.")
                    Button(onClick = { workspace = "local" }) { Text("On this device") }
                    Text("No sign-in needed. Local boards stay on this device.")
                    Button(onClick = { workspace = "cloud" }) { Text("Shared boards") }
                    Text("Sign in to use existing Firebase boards.")
                }
            } else if (workspace == "local") {
                LocalWorkspaceRoute(onExit = { workspace = null })
            } else {
            BackHandler { workspace = null }
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
}
