package ru.dohod.moidohod.ui.screens.payments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.dohod.moidohod.data.entity.*
import ru.dohod.moidohod.data.repository.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

data class PaymentUiState(
    val upcomingPayments: List<UpcomingPayment> = emptyList(),
    val paymentHistory: List<PaymentHistoryGroup> = emptyList(),
    val isLoading: Boolean = false
)

data class UpcomingPayment(
    val id: String,
    val title: String,
    val date: LocalDate,
    val amount: Double,
    val description: String = "",
    val isManual: Boolean = false,
    val isPast: Boolean = false
)

data class PaymentHistoryGroup(
    val month: YearMonth,
    val payments: List<Payment>,
    val total: Double
)

class PaymentsViewModel(
    private val paymentRepository: PaymentRepository,
    private val settingsRepository: SettingsRepository,
    private val workDayRepository: WorkDayRepository,
    private val completedTaskRepository: CompletedTaskRepository
) : ViewModel() {

    var uiState by mutableStateOf(PaymentUiState())
        private set

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadData()
    }

    fun refresh() {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            try {
                val settings = settingsRepository.getSettings().first()
                    ?: Settings().also { settingsRepository.saveSettings(it) }

                launch {
                    paymentRepository.getAllPayments().collect { payments ->
                        val grouped = payments
                            .filter { it.isActual }
                            .groupBy { YearMonth.parse(it.date.substring(0, 7)) }
                            .map { (month, list) ->
                                PaymentHistoryGroup(
                                    month = month,
                                    payments = list.sortedByDescending { it.date },
                                    total = list.sumOf { it.amount }
                                )
                            }
                            .sortedByDescending { it.month }
                        uiState = uiState.copy(paymentHistory = grouped)
                    }
                }

                val upcoming = calculateUpcomingPayments(settings)
                uiState = uiState.copy(upcomingPayments = upcoming, isLoading = false)

            } catch (e: Exception) {
                e.printStackTrace()
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    private suspend fun calculateUpcomingPayments(settings: Settings): List<UpcomingPayment> {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        val previousMonth = currentMonth.minusMonths(1)

        val upcoming = mutableListOf<UpcomingPayment>()

        // ===== Аванс 5-го числа =====
        val advanceDate = LocalDate.of(currentMonth.year, currentMonth.month, 5)
        val advanceBase = calculateSalaryForPeriod(
            start = previousMonth.atDay(16),
            end = previousMonth.atEndOfMonth(),
            settings = settings,
            applyTax = true
        )
        // Доплаты: амортизация авто + разъездной характер (после налога)
        val extrasNet = (settings.carDepreciation + settings.travelCompensation) *
                (1 - settings.taxRatePercent / 100.0)
        val advanceAmount = advanceBase + extrasNet

        upcoming.add(
            UpcomingPayment(
                id = "advance-${currentMonth}",
                title = "Аванс (5 ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("ru"))})",
                date = advanceDate,
                amount = advanceAmount,
                description = "оклад за 16–${previousMonth.month.getDisplayName(TextStyle.FULL, Locale("ru"))} + доплаты",
                isPast = advanceDate <= today
            )
        )

        // ===== Зарплата 20-го числа =====
        val salaryDate = LocalDate.of(currentMonth.year, currentMonth.month, 20)

        // Оклад за 1-15 без налога
        val salaryPartGross = calculateSalaryForPeriod(
            start = currentMonth.atDay(1),
            end = currentMonth.atDay(15),
            settings = settings,
            applyTax = false
        )

        // Премия за прошлый месяц
        val previousMonthStr = previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val earnedPoints = completedTaskRepository.getTotalPointsForMonth(previousMonthStr)

        val previousMonthWorkDaysAll = workDayRepository.getDaysForMonthSync(previousMonthStr)
            .filter { it.type == DayType.WORK }
        val planPointsPrevious: Double = previousMonthWorkDaysAll.size * settings.dailyBonusNorm

        val bonusPercentRaw = if (planPointsPrevious > 0.0) {
            (earnedPoints / planPointsPrevious) * 100
        } else 0.0
        val bonusPercent = bonusPercentRaw.coerceIn(0.0, 150.0)

        val hourRate = if (settings.yearNormHours > 0) {
            (settings.salary * 12) / settings.yearNormHours
        } else 0.0
        val previousMonthSalaryGross = hourRate * (previousMonthWorkDaysAll.size * settings.shiftHours)

        val bonusAmount = if (bonusPercent >= 60) {
            previousMonthSalaryGross * (bonusPercent / 100.0)
        } else 0.0

        val totalGross = salaryPartGross + bonusAmount
        val netAmount = totalGross * (1 - settings.taxRatePercent / 100.0)

        val description = if (bonusAmount > 0) {
            "оклад 1–15 + премия ${bonusPercent.toInt()}% за ${previousMonth.month.getDisplayName(TextStyle.FULL, Locale("ru"))}"
        } else {
            "оклад 1–15 (премия не начислена)"
        }

        upcoming.add(
            UpcomingPayment(
                id = "salary-${currentMonth}",
                title = "Зарплата (20 ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("ru"))})",
                date = salaryDate,
                amount = netAmount,
                description = description,
                isPast = salaryDate <= today
            )
        )

        return upcoming
    }

    private suspend fun calculateSalaryForPeriod(
        start: LocalDate,
        end: LocalDate,
        settings: Settings,
        applyTax: Boolean = true
    ): Double {
        val workedDays = getWorkedDaysForPeriod(start, end)
        val workedHours = workedDays * settings.shiftHours
        val hourRate = if (settings.yearNormHours > 0) {
            (settings.salary * 12) / settings.yearNormHours
        } else 0.0
        val gross = hourRate * workedHours
        val net = gross * (1 - settings.taxRatePercent / 100.0)
        return if (applyTax) net else gross
    }

    private suspend fun getWorkedDaysForPeriod(start: LocalDate, end: LocalDate): Int {
        var count = 0
        var date = start
        while (date <= end) {
            val dateStr = date.format(dateFormatter)
            val day = workDayRepository.getDay(dateStr)
            if (day?.type == DayType.WORK) count++
            date = date.plusDays(1)
        }
        return count
    }

    // ===== Фиксация фактической выплаты в истории =====
    fun markPaymentAsReceived(
        paymentType: PaymentType,
        date: LocalDate,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            val payment = Payment(
                date = date.format(dateFormatter),
                type = paymentType,
                amount = amount,
                description = description,
                isActual = true
            )
            paymentRepository.insert(payment)
        }
    }

    fun addManualPayment(date: LocalDate, amount: Double, description: String) {
        viewModelScope.launch {
            val payment = Payment(
                date = date.format(dateFormatter),
                type = PaymentType.MANUAL,
                amount = amount,
                description = description,
                isActual = true
            )
            paymentRepository.insert(payment)
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            if (payment.type == PaymentType.MANUAL) {
                paymentRepository.delete(payment)
            }
        }
    }
}