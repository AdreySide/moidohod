package ru.dohod.moidohod.ui.screens.payments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
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
    val isManual: Boolean = false
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

            val settings = settingsRepository.getSettings().first()
                ?: Settings().also { settingsRepository.saveSettings(it) }

            loadPaymentHistory()
            val upcoming = calculateUpcomingPayments(settings)
            uiState = uiState.copy(upcomingPayments = upcoming, isLoading = false)
        }
    }

    private suspend fun loadPaymentHistory() {
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

    private suspend fun calculateUpcomingPayments(settings: Settings): List<UpcomingPayment> {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)
        val previousMonth = currentMonth.minusMonths(1)

        val upcoming = mutableListOf<UpcomingPayment>()

        // ---- Аванс 5-го числа ----
        val advanceDate = LocalDate.of(currentMonth.year, currentMonth.month, 5)
        if (advanceDate >= today) {
            val periodStart = previousMonth.atDay(16)
            val periodEnd = previousMonth.atEndOfMonth()
            val advanceAmount = calculateSalaryForPeriod(periodStart, periodEnd, settings)
            upcoming.add(
                UpcomingPayment(
                    id = "advance-${currentMonth}",
                    title = "Аванс (5 ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("ru"))})",
                    date = advanceDate,
                    amount = advanceAmount
                )
            )
        }

        // ---- Зарплата 20-го числа ----
        val salaryDate = LocalDate.of(currentMonth.year, currentMonth.month, 20)
        if (salaryDate >= today) {
            // Оклад за 1-15 число текущего месяца
            val salaryPeriodStart = currentMonth.atDay(1)
            val salaryPeriodEnd = currentMonth.atDay(15)
            val salaryPart = calculateSalaryForPeriod(salaryPeriodStart, salaryPeriodEnd, settings)

            // Премия за прошлый месяц
            val previousMonthStr = previousMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val earnedPoints = completedTaskRepository.getTotalPointsForMonth(previousMonthStr)
            val planPoints = calculatePlanPointsForMonth(previousMonth, settings.dailyBonusNorm)

            val bonusPercentRaw = if (planPoints > 0) {
                ((earnedPoints.toDouble() / planPoints) * 100).coerceIn(0.0, 150.0)
            } else 0.0

            val bonusAmount = if (bonusPercentRaw >= 60) {
                val previousMonthWorkedDays = getWorkedDaysForPeriod(
                    previousMonth.atDay(1),
                    previousMonth.atEndOfMonth()
                )
                val previousMonthWorkedHours = previousMonthWorkedDays * settings.shiftHours
                val hourRate = (settings.salary * 12) / settings.yearNormHours
                val previousMonthSalary = hourRate * previousMonthWorkedHours
                previousMonthSalary * (bonusPercentRaw / 100.0)
            } else 0.0

            val netAmount = (salaryPart + bonusAmount) * (1 - settings.taxRatePercent / 100.0)
            val description = if (bonusAmount > 0) {
                "Оклад за 1-15 + премия ${bonusPercentRaw.toInt()}% за ${previousMonth.month.getDisplayName(TextStyle.FULL, Locale("ru"))}"
            } else {
                "Оклад за 1-15 (премия не начислена)"
            }

            upcoming.add(
                UpcomingPayment(
                    id = "salary-${currentMonth}",
                    title = "Зарплата (20 ${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("ru"))})",
                    date = salaryDate,
                    amount = netAmount,
                    description = description
                )
            )
        }

        return upcoming
    }

    private suspend fun calculateSalaryForPeriod(start: LocalDate, end: LocalDate, settings: Settings): Double {
        val workedDays = getWorkedDaysForPeriod(start, end)
        val workedHours = workedDays * settings.shiftHours
        val hourRate = (settings.salary * 12) / settings.yearNormHours
        val gross = hourRate * workedHours
        return gross * (1 - settings.taxRatePercent / 100.0)
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

    private fun calculatePlanPointsForMonth(yearMonth: YearMonth, dailyNorm: Double): Int {
        var workDays = 0
        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            if (date.dayOfWeek.value in 1..5) workDays++
        }
        return (workDays * dailyNorm).toInt()
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