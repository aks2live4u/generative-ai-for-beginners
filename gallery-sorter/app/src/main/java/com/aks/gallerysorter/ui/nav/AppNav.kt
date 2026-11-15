package com.aks.gallerysorter.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.aks.gallerysorter.ui.dupes.DuplicatesScreen
import com.aks.gallerysorter.ui.grid.GridScreen
import com.aks.gallerysorter.ui.grid.GridSource
import com.aks.gallerysorter.ui.home.HomeActions
import com.aks.gallerysorter.ui.home.HomeScreen
import com.aks.gallerysorter.ui.label
import com.aks.gallerysorter.ui.onboarding.OnboardingScreen
import com.aks.gallerysorter.ui.onboarding.OnboardingViewModel
import com.aks.gallerysorter.ui.people.PeopleScreen
import com.aks.gallerysorter.ui.places.PlacesScreen
import com.aks.gallerysorter.ui.review.ReviewScreen
import com.aks.gallerysorter.ui.settings.AboutScreen
import com.aks.gallerysorter.ui.settings.PastPhonesScreen
import com.aks.gallerysorter.ui.settings.SettingsScreen
import com.aks.gallerysorter.ui.vault.VaultScreen
import com.aks.gallerysorter.ui.vault.VaultViewerScreen
import com.aks.gallerysorter.ui.viewer.ViewerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Route names. */
object Routes {
    const val HOME = "home"
    const val GRID = "grid/{source}"
    const val VIEWER = "viewer/{source}/{index}"
    const val VAULT = "vault"
    const val VAULT_VIEWER = "vaultviewer/{tier}/{index}"
    const val ONBOARDING = "onboarding"
    const val PEOPLE = "people"
    const val PLACES = "places"
    const val REVIEW = "review"
    const val DUPLICATES = "duplicates"
    const val SETTINGS = "settings"
    const val PAST_PHONES = "pastphones"
    const val ABOUT = "about"

    fun grid(source: GridSource) = "grid/${source.encode()}"
    fun viewer(source: GridSource, index: Int) = "viewer/${source.encode()}/$index"
    fun vaultViewer(tier: VaultTier, index: Int) = "vaultviewer/${tier.name}/$index"
}

/** The app's screens. Until onboarding has been completed, it starts there instead of Home. */
@Composable
fun AppNav(nav: NavHostController = rememberNavController()) {
    val graph = LocalContext.current.graph
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { graph.notices.collect { snackbar.showSnackbar(it) } }
    // Read on IO (the index must not open on the main thread); nothing is shown until it is known.
    val start by produceState<String?>(null) {
        value = withContext(Dispatchers.IO) { if (OnboardingViewModel.isDone(graph.db.settingsDao())) Routes.HOME else Routes.ONBOARDING }
    }
    val startRoute = start ?: return

    Box(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = startRoute) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } } })
            }
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
                    is GridSource.OfCity -> source.city
                    else -> produceState("", source) {
                        value = withContext(Dispatchers.IO) {
                            when (source) {
                                is GridSource.OfAlbum -> graph.db.albumDao().get(source.albumId)?.name ?: "Album"
                                is GridSource.OfPerson -> graph.db.peopleDao().person(source.personId)?.let { it.name ?: if (it.isMe) "Me" else null } ?: "Person"
                                is GridSource.OfTrip -> graph.scanSettings.trips().firstOrNull { it.startMs == source.startMs }?.name ?: "Trip"
                                else -> ""
                            }
                        }
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
            composable(Routes.PEOPLE) {
                PeopleScreen(onOpen = { nav.navigate(Routes.grid(GridSource.OfPerson(it))) }, onBack = { nav.popBackStack() })
            }
            composable(Routes.PLACES) { PlacesScreen(onOpen = { nav.navigate(Routes.grid(it)) }, onBack = { nav.popBackStack() }) }
            composable(Routes.REVIEW) { ReviewScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.DUPLICATES) { DuplicatesScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onPastPhones = { nav.navigate(Routes.PAST_PHONES) },
                    onAbout = { nav.navigate(Routes.ABOUT) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.PAST_PHONES) { PastPhonesScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
