package ru.dohod.moidohod.ui.screens.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.dohod.moidohod.data.repository.*

class PaymentsViewModelFactory(
    private val paymentRepository: PaymentRepository,
    private val settingsRepository: SettingsRepository,
    private val workDayRepository: WorkDayRepository,
    private val completedTaskRepository: CompletedTaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentsViewModel(
                paymentRepository,
                settingsRepository,
                workDayRepository,
                completedTaskRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}