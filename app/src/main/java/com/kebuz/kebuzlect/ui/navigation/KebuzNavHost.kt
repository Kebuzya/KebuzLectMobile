package com.kebuz.kebuzlect.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kebuz.kebuzlect.ui.album.AlbumScreen
import com.kebuz.kebuzlect.ui.albums.AlbumsScreen
import com.kebuz.kebuzlect.ui.buckets.BucketPickerScreen
import com.kebuz.kebuzlect.ui.settings.SettingsScreen

@Composable
fun KebuzNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destinations.ALBUMS) {
        composable(Destinations.ALBUMS) {
            AlbumsScreen(
                onOpenAlbum = { bucketId -> navController.navigate(Destinations.album(bucketId)) },
                onAddAlbum = { navController.navigate(Destinations.BUCKET_PICKER) },
                onOpenSettings = { navController.navigate(Destinations.SETTINGS) },
            )
        }
        composable(Destinations.BUCKET_PICKER) {
            BucketPickerScreen(
                onAdded = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Destinations.ALBUM,
            arguments = listOf(navArgument(Destinations.ALBUM_ARG_BUCKET_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getString(Destinations.ALBUM_ARG_BUCKET_ID).orEmpty()
            AlbumScreen(bucketId = bucketId, onBack = { navController.popBackStack() })
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
