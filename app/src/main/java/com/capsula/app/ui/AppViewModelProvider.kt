package com.capsula.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.capsula.app.CapsulaApplication
import com.capsula.app.ui.create.CreateCapsuleViewModel
import com.capsula.app.ui.detail.CapsuleDetailViewModel
import com.capsula.app.ui.insights.InsightsViewModel
import com.capsula.app.ui.vault.VaultViewModel

// Провайдер фабрики ViewModel для приложения Capsula. Обеспечивает создание экземпляров ViewModel с необходимыми зависимостями.
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            VaultViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = capsulaApp().container.capsuleRepository,
                ticker = capsulaApp().container.ticker,
            )
        }
        initializer {
            CreateCapsuleViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = capsulaApp().container.capsuleRepository,
                timeProvider = capsulaApp().container.timeProvider,
            )
        }
        initializer {
            CapsuleDetailViewModel(
                savedStateHandle = createSavedStateHandle(),
                repository = capsulaApp().container.capsuleRepository,
                ticker = capsulaApp().container.ticker,
                timeProvider = capsulaApp().container.timeProvider,
            )
        }
        initializer {
            InsightsViewModel(
                repository = capsulaApp().container.capsuleRepository,
                ticker = capsulaApp().container.ticker,
            )
        }
    }
}

private fun CreationExtras.capsulaApp(): CapsulaApplication =
    this[AndroidViewModelFactory.APPLICATION_KEY] as CapsulaApplication
