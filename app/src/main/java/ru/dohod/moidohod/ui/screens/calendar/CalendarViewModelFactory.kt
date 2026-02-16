package ru.dohod.moidohod.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.dohod.moidohod.data.dao.WorkDayDao
import ru.dohod.moidohod.data.repository.SettingsRepository
import ru.dohod.moidohod.data.repository.WorkDayRepository

class CalendarViewModelFactory(
    private val workDayDao: WorkDayDao,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(
                WorkDayRepository(workDayDao),
                settingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}