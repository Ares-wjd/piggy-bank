package io.github.areswjd.piggybank.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.session.SessionState
import io.github.areswjd.piggybank.ui.assets.AssetDetailScreen
import io.github.areswjd.piggybank.ui.assets.AssetDetailViewModel
import io.github.areswjd.piggybank.ui.assets.AssetsScreen
import io.github.areswjd.piggybank.ui.ledger.LedgerScreen
import io.github.areswjd.piggybank.ui.session.LoginScreen
import io.github.areswjd.piggybank.ui.session.SessionViewModel
import io.github.areswjd.piggybank.ui.settings.SettingsScreen
import io.github.areswjd.piggybank.ui.transaction.TransactionEditScreen
import io.github.areswjd.piggybank.ui.transaction.TransactionEditViewModel

/** 하단 탭으로 이동하는 최상위 화면. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    LEDGER("ledger", R.string.tab_ledger, Icons.AutoMirrored.Rounded.ReceiptLong),
    ASSETS("assets", R.string.tab_assets, Icons.Rounded.Savings),
    SETTINGS("settings", R.string.tab_settings, Icons.Rounded.Settings),
}

private object Routes {
    const val TRANSACTION = "transaction?${TransactionEditViewModel.ARG_ID}={${TransactionEditViewModel.ARG_ID}}"
    const val ASSET_DETAIL = "asset/{${AssetDetailViewModel.ARG_ASSET_ID}}"

    fun transaction(id: Long? = null) = if (id == null) "transaction" else "transaction?${TransactionEditViewModel.ARG_ID}=$id"
    fun assetDetail(id: Long) = "asset/$id"
}

/** 로그인 여부에 따라 로그인 화면 또는 메인 화면을 보여준다. */
@Composable
fun PiggyBankRoot(sessionViewModel: SessionViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()
    when (state) {
        SessionState.Loading -> Box(Modifier.fillMaxSize())
        SessionState.SignedOut -> LoginScreen(sessionViewModel)
        is SessionState.SignedIn -> PiggyBankApp()
    }
}

@Composable
fun PiggyBankApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val onTopLevel = TopLevelDestination.entries.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        // 상태 표시줄 여백은 각 화면의 TopAppBar가 처리한다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (onTopLevel) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.LEDGER.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(TopLevelDestination.LEDGER.route) {
                LedgerScreen(
                    onAddTransaction = { navController.navigate(Routes.transaction()) },
                    onTransactionClick = { navController.navigate(Routes.transaction(it)) },
                )
            }
            composable(TopLevelDestination.ASSETS.route) {
                AssetsScreen(onAssetClick = { navController.navigate(Routes.assetDetail(it)) })
            }
            composable(TopLevelDestination.SETTINGS.route) { SettingsScreen() }
            composable(
                Routes.TRANSACTION,
                arguments = listOf(
                    navArgument(TransactionEditViewModel.ARG_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) {
                TransactionEditScreen(onDone = { navController.popBackStack() })
            }
            composable(
                Routes.ASSET_DETAIL,
                arguments = listOf(navArgument(AssetDetailViewModel.ARG_ASSET_ID) { type = NavType.LongType }),
            ) {
                AssetDetailScreen(
                    onBack = { navController.popBackStack() },
                    onTransactionClick = { navController.navigate(Routes.transaction(it)) },
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
