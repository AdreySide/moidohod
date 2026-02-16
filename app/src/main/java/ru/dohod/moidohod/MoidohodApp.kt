package ru.dohod.moidohod

import android.app.Application
import ru.dohod.moidohod.data.database.AppDatabase
import ru.dohod.moidohod.data.repository.*

class MoidohodApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(database.settingsDao())
    }

    val workDayRepository: WorkDayRepository by lazy {
        WorkDayRepository(database.workDayDao())
    }

    val taskTypeRepository: TaskTypeRepository by lazy {
        TaskTypeRepository(database.taskTypeDao())
    }

    val completedTaskRepository: CompletedTaskRepository by lazy {
        CompletedTaskRepository(database.completedTaskDao())
    }
    val paymentRepository: PaymentRepository by lazy {
        PaymentRepository(database.paymentDao())
    }
}