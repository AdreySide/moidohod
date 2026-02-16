package ru.dohod.moidohod.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.dohod.moidohod.data.entity.Settings
import ru.dohod.moidohod.data.repository.SettingsRepository

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<Settings?>(null)
    val settings: StateFlow<Settings?> = _settings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveCompleted = MutableStateFlow(false)
    val saveCompleted: StateFlow<Boolean> = _saveCompleted.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.getSettings().collect { settings ->
                _settings.value = settings
                _isLoading.value = false
            }
        }
    }

    fun saveSettings(
        salary: Double,
        yearNormHours: Int,
        dailyBonusNorm: Double,
        taxRatePercent: Int
    ) {
        viewModelScope.launch {
            val current = _settings.value
            val settingsToSave = current?.copy(
                salary = salary,
                yearNormHours = yearNormHours,
                dailyBonusNorm = dailyBonusNorm,
                taxRatePercent = taxRatePercent
            ) ?: Settings(
                salary = salary,
                yearNormHours = yearNormHours,
                dailyBonusNorm = dailyBonusNorm,
                taxRatePercent = taxRatePercent
            )
            repository.saveSettings(settingsToSave)
            _saveCompleted.value = true
            kotlinx.coroutines.delay(2000)
            _saveCompleted.value = false
        }
    }
}