package io.github.areswjd.piggybank.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.areswjd.piggybank.AppContainer
import io.github.areswjd.piggybank.PiggyBankApplication
import io.github.areswjd.piggybank.ui.assets.AssetDetailViewModel
import io.github.areswjd.piggybank.ui.assets.AssetsViewModel
import io.github.areswjd.piggybank.ui.design.DesignViewModel
import io.github.areswjd.piggybank.ui.ledger.LedgerViewModel
import io.github.areswjd.piggybank.ui.session.SessionViewModel
import io.github.areswjd.piggybank.ui.settings.SettingsViewModel
import io.github.areswjd.piggybank.ui.transaction.TransactionEditViewModel
import io.github.areswjd.piggybank.ui.update.UpdateViewModel

/** 모든 화면의 ViewModel을 [AppContainer]의 객체로 만든다. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { SessionViewModel(container()) }
        initializer { LedgerViewModel(container().transactionRepository, container().preferences) }
        initializer {
            TransactionEditViewModel(
                createSavedStateHandle(),
                container().transactionRepository,
                container().assetRepository,
                container().preferences,
            )
        }
        initializer { AssetsViewModel(container().assetRepository) }
        initializer {
            AssetDetailViewModel(createSavedStateHandle(), container().assetRepository, container().transactionRepository)
        }
        initializer {
            SettingsViewModel(
                container().session,
                container().backupManager,
                container().updateManager,
                container().preferences,
                container().appIconManager,
            )
        }
        initializer { DesignViewModel(container().preferences, container().appIconManager) }
        initializer { UpdateViewModel(container().updateManager) }
    }
}

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as PiggyBankApplication).container
