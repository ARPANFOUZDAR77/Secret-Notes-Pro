package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.fillMaxSize
import com.example.data.AppDatabase
import com.example.data.NoteRepository
import com.example.data.SecurityPreferences
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.PinSetupScreen
import com.example.ui.home.HomeScreen
import com.example.ui.note.NoteScreen
import com.example.ui.SharedViewModel
import com.example.ui.SharedViewModelFactory

import com.example.ui.settings.SettingsScreen

@Composable
fun AppNavigation(activity: FragmentActivity) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val securityPrefs = remember { SecurityPreferences(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { NoteRepository(database.noteDao()) }
    
    val factory = SharedViewModelFactory(repository, securityPrefs)
    val sharedViewModel: SharedViewModel = viewModel(factory = factory)
    
    val hasPinState = securityPrefs.hashedPinFlow.collectAsState(initial = "LOADING")
    
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            val hasPin = hasPinState.value
            androidx.compose.runtime.LaunchedEffect(hasPin) {
                if (hasPin != "LOADING") {
                    if (hasPin == null) {
                        navController.navigate("setup") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("auth") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            }
            androidx.compose.foundation.layout.Box(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
        }

        composable("setup") {
            PinSetupScreen(
                sharedViewModel = sharedViewModel,
                onSetupComplete = {
                    navController.navigate("home") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
        
        composable("auth") {
            AuthScreen(
                activity = activity,
                sharedViewModel = sharedViewModel,
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                sharedViewModel = sharedViewModel,
                onNavigateToNote = { noteId ->
                    navController.navigate("note/$noteId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable("note/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toIntOrNull() ?: -1
            NoteScreen(
                noteId = noteId,
                sharedViewModel = sharedViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                sharedViewModel = sharedViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
