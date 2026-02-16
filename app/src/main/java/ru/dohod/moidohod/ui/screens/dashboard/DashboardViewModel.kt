package ru.dohod.moidohod.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.dohod.moidohod.data.entity.DayType
import ru.dohod.moidohod.data.entity.Settings
import ru.dohod.moidohod.data.repository.CompletedTaskRepository
import ru.dohod.moidohod.data.repository.SettingsRepository
import ru.dohod.moidohod.data.repository.WorkDayRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class DashboardData(
    val bonusPercent: Int = 0,
    val earnedPoints: Int = 0,
    val planPoints: Int = 0,
    val remainingWorkDays: Int = 0,
    val workedDays: Int = 0,
    val workedHours: Int = 0,
    val monthNormHours: Int = 0,
    val salaryBase: Double = 0.0,
    val bonusAmount: Double = 0.0,
    val total: Double = 0.0
)

class DashboardViewModel(
    private val settingsRepository: SettingsRepository,
    private val workDayRepository: WorkDayRepository,
    private val completedTaskRepository: CompletedTaskRepository
) : ViewModel() {

    var dashboardData by mutableStateOf(DashboardData())
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val currentYearMonth = YearMonth.now()
    private val yearMonthStr = currentYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    init {
        observeData()
    }

    fun refresh() {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            isLoading = true

            combine(
                settingsRepository.getSettings(),
                workDayRepository.getDaysForMonthFlow(yearMonthStr),
                completedTaskRepository.getTotalPointsForMonthFlow(yearMonthStr)
            ) { settings, workDays, earnedPoints ->
                val currentSettings = settings ?: Settings().also {
                    settingsRepository.saveSettings(it)
                }
                calculateDashboardData(currentSettings, workDays, earnedPoints)
            }.collect { data ->
                dashboardData = data
                isLoading = false
            }
        }
    }

    private fun calculateDashboardData(
        settings: Settings,
        workDays: List<ru.dohod.moidohod.data.entity.WorkDay>,
        earnedPoints: Int
    ): DashboardData {
        val today = LocalDate.now()
        val yearMonth = YearMonth.from(today)

        // Отработанные дни (WORK и дата ≤ сегодня)
        val workedDays = workDays.filter {
            it.type == DayType.WORK && LocalDate.parse(it.date, dateFormatter) <= today
        }.size
        val workedHours = workedDays * settings.shiftHours

        // Норма часов за месяц
        val monthNormHours = (settings.yearNormHours / 12).coerceAtLeast(1)

        // Окладная часть
        val hourRate = (settings.salary * 12) / settings.yearNormHours
        val salaryBase = hourRate * workedHours

        // План баллов на месяц (все будние дни)
        val totalWorkDaysInMonth = (1..yearMonth.lengthOfMonth()).count { day ->
            yearMonth.atDay(day).dayOfWeek.value in 1..5
        }
        val planPointsDouble = totalWorkDaysInMonth * settings.dailyBonusNorm
        val planPoints = planPointsDouble.toInt()

        // Процент выполнения плана (для кружка) — 0..150
        val bonusPercent = if (planPointsDouble > 0) {
            ((earnedPoints / planPointsDouble) * 100)
                .coerceIn(0.0, 150.0)
                .toInt()
        } else 0

        // Сумма премии к начислению — только если процент ≥ 60
        val bonusAmount = if (bonusPercent >= 60) {
            salaryBase * (bonusPercent / 100.0)
        } else 0.0

        // Итого: оклад + премия (если она >0, иначе только оклад)
        val total = salaryBase + bonusAmount

        // Остаток рабочих дней
        val remainingWorkDays = if (today.dayOfMonth < yearMonth.lengthOfMonth()) {
            (today.dayOfMonth + 1..yearMonth.lengthOfMonth()).count { day ->
                yearMonth.atDay(day).dayOfWeek.value in 1..5
            }
        } else 0

        return DashboardData(
            bonusPercent = bonusPercent,
            earnedPoints = earnedPoints,
            planPoints = planPoints,
            remainingWorkDays = remainingWorkDays,
            workedDays = workedDays,
            workedHours = workedHours,
            monthNormHours = monthNormHours,
            salaryBase = salaryBase,
            bonusAmount = bonusAmount,
            total = total
        )
    }
}