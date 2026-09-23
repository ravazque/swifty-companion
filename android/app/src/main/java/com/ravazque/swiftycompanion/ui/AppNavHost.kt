package com.ravazque.swiftycompanion.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ravazque.swiftycompanion.ui.profile.ProfileScreen
import com.ravazque.swiftycompanion.ui.search.SearchScreen
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

@Serializable
data class ProfileDestination(val login: String)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SearchDestination) {
        composable<SearchDestination> {
            SearchScreen(
                onProfileFound = { login ->
                    navController.navigate(ProfileDestination(login)) { launchSingleTop = true }
                },
            )
        }
        composable<ProfileDestination> {
            ProfileScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
    }
}
