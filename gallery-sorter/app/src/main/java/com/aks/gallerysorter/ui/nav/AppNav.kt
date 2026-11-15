package com.aks.gallerysorter.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aks.gallerysorter.core.VaultTier
import com.aks.gallerysorter.graph
import com.aks.gallerysorter.ui.grid.GridScreen
import com.aks.gallerysorter.ui.grid.GridSource
import com.aks.gallerysorter.ui.home.HomeActions
import com.aks.gallerysorter.ui.home.HomeScreen
import com.aks.gallerysorter.ui.label
import com.aks.gallerysorter.ui.vault.VaultScreen
import com.aks.gallerysorter.ui.vault.VaultViewerScreen
import com.aks.gallerysorter.ui.viewer.ViewerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Route names. Screens marked 14b are placeholders until Task 14b replaces them. */
object Routes {
    const val HOME = "home"
    const val GRID = "grid/{source}"
    const val VIEWER = "viewer/{source}/{index}"
    const val VAULT = "vault"
    const val VAULT_VIEWER = "vaultviewer/{tier}/{index}"
    const val ONBOARDING = "onboarding" // 14b
    const val PEOPLE = "people" // 14b
    const val PLACES = "places"
    const val REVIEW = "review" // 14b
    const val DUPLICATES = "duplicates" // 14b
    const val SETTINGS = "settings" // 14b
    const val ABOUT = "about" // 14b

    fun grid(source: GridSource) = "grid/${source.encode()}"
    fun viewer(source: GridSource, index: Int) = "viewer/${source.encode()}/$index"
    fun vaultViewer(tier: VaultTier, index: Int) = "vaultviewer/${tier.name}/$index"
}

@Composable
fun AppNav(nav: NavHostController = rememberNavController()) {
    val graph = LocalContext.current.graph
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { graph.notices.collect { snackbar.showSnackbar(it) } }

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    HomeActions(
                        category = { nav.navigate(Routes.grid(GridSource.OfCategory(it))) },
                        album = { nav.navigate(Routes.grid(GridSource.OfAlbum(it))) },
                        people = { nav.navigate(Routes.PEOPLE) },
                        places = { nav.navigate(Routes.PLACES) },
                        duplicates = { nav.navigate(Routes.DUPLICATES) },
                        review = { nav.navigate(Routes.REVIEW) },
                        vault = { nav.navigate(Routes.VAULT) },
                        settings = { nav.navigate(Routes.SETTINGS) },
                    ),
                )
            }
            composable(Routes.GRID, listOf(navArgument("source") { type = NavType.StringType })) { entry ->
                val source = GridSource.decode(entry.arguments?.getString("source").orEmpty())
                val title = when (source) {
                    is GridSource.OfCategory -> source.category.label
                    is GridSource.OfAlbum -> produceState("Album", source.albumId) {
                        value = withContext(Dispatchers.IO) { graph.db.albumDao().get(source.albumId)?.name } ?: "Album"
                    }.value
                }
                GridScreen(
                    source = source,
                    title = title,
                    onOpen = { nav.navigate(Routes.viewer(source, it)) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                Routes.VIEWER,
                listOf(navArgument("source") { type = NavType.StringType }, navArgument("index") { type = NavType.IntType }),
            ) { entry ->
                ViewerScreen(
                    source = GridSource.decode(entry.arguments?.getString("source").orEmpty()),
                    start = entry.arguments?.getInt("index") ?: 0,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.VAULT) {
                VaultScreen(onOpen = { tier, i -> nav.navigate(Routes.vaultViewer(tier, i)) }, onBack = { nav.popBackStack() })
            }
            composable(
                Routes.VAULT_VIEWER,
                listOf(navArgument("tier") { type = NavType.StringType }, navArgument("index") { type = NavType.IntType }),
            ) { entry ->
                VaultViewerScreen(
                    tier = VaultTier.valueOf(entry.arguments?.getString("tier").orEmpty()),
                    start = entry.arguments?.getInt("index") ?: 0,
                    onBack = { nav.popBackStack() },
                )
            }
            placeholder(nav, Routes.ONBOARDING, "Welcome")
            placeholder(nav, Routes.PEOPLE, "People")
            placeholder(nav, Routes.PLACES, "Places")
            placeholder(nav, Routes.REVIEW, "Review")
            placeholder(nav, Routes.DUPLICATES, "Duplicates")
            placeholder(nav, Routes.SETTINGS, "Settings")
            placeholder(nav, Routes.ABOUT, "About")
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

private fun androidx.navigation.NavGraphBuilder.placeholder(nav: NavHostController, route: String, title: String) {
    composable(route) { ComingSoon(title) { nav.popBackStack() } }
}
