package ru.dohod.moidohod.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.dohod.moidohod.data.repository.CompletedTaskRepository
import ru.dohod.moidohod.data.repository.SettingsRepository
import ru.dohod.moidohod.data.repository.WorkDayRepository

class DashboardViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val workDayRepository: WorkDayRepository,
    private val completedTaskRepository: CompletedTaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                settingsRepository,
                workDayRepository,
                completedTaskRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}