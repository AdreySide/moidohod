package ru.dohod.moidohod.data.repository

import kotlinx.coroutines.flow.Flow
import ru.dohod.moidohod.data.dao.SettingsDao
import ru.dohod.moidohod.data.entity.Settings

class SettingsRepository(private val settingsDao: SettingsDao) {
    fun getSettings(): Flow<Settings?> = settingsDao.getSettings()

    suspend fun saveSettings(settings: Settings) {
        settingsDao.insert(settings)
    }
}