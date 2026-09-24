package ru.dohod.moidohod.ui.screens.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
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
    val earnedPoints: Double = 0.0,
    val planPoints: Double = 0.0,
    val remainingWorkDays: Int = 0,
    val workedDays: Int = 0,
    val workedHours: Int = 0,
    val monthNormHours: Int = 0,
    val salaryBase: Double = 0.0,
    val bonusAmount: Double = 0.0,
    val total: Double = 0.0,
    val advance5Amount: Double = 0.0,
    val salary20Amount: Double = 0.0
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
    private val previousYearMonth = currentYearMonth.minusMonths(1)
    private val previousMonthStr = previousYearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))

    init { observeData() }

    fun refresh() { observeData() }

    private fun observeData() {
        viewModelScope.launch {
            isLoading = true
            val settingsFlow = settingsRepository.getSettings()
            val currentMonthFlow = workDayRepository.getDaysForMonthFlow(yearMonthStr)
            val currentPointsFlow = completedTaskRepository.getTotalPointsForMonthFlow(yearMonthStr)
            val previousMonthFlow = workDayRepository.getDaysForMonthFlow(previousMonthStr)
            val previousPointsFlow = completedTaskRepository.getTotalPointsForMonthFlow(previousMonthStr)

            combine(
                settingsFlow,
                currentMonthFlow,
                currentPointsFlow,
                previousMonthFlow,
                previousPointsFlow
            ) { settings, workDays, earnedPoints, previousWorkDays, previousPoints ->
                val currentSettings = settings ?: Settings().also { settingsRepository.saveSettings(it) }
                calculateDashboardData(
                    settings = currentSettings,
                    workDays = workDays,
                    earnedPoints = earnedPoints,
                    previousMonthWorkDays = previousWorkDays,
                    previousMonthPoints = previousPoints
                )
            }.collect { data ->
                dashboardData = data
                isLoading = false
            }
        }
    }

    private fun calculateDashboardData(
        settings: Settings,
        workDays: List<ru.dohod.moidohod.data.entity.WorkDay>,
        earnedPoints: Double,
        previousMonthWorkDays: List<ru.dohod.moidohod.data.entity.WorkDay>,
        previousMonthPoints: Double
    ): DashboardData {
        val today = LocalDate.now()

        val allWorkDays = workDays.filter { it.type == DayType.WORK }
        val workedDays = allWorkDays.count {
            LocalDate.parse(it.date, dateFormatter) <= today
        }
        val planWorkDays = allWorkDays.size

        val workedHours = workedDays * settings.shiftHours
        val monthNormHours = planWorkDays * settings.shiftHours

        val hourRate = if (settings.yearNormHours > 0) {
            (settings.salary * 12) / settings.yearNormHours
        } else 0.0

        val salaryBase = hourRate * workedHours

        // planPoints — Double
        val planPoints: Double = planWorkDays * settings.dailyBonusNorm

        val bonusPercent = if (planPoints > 0.0) {
            ((earnedPoints / planPoints) * 100).coerceIn(0.0, 150.0).toInt()
        } else 0

        val bonusAmount = if (bonusPercent >= 60) {
            salaryBase * (bonusPercent / 100.0)
        } else 0.0

        val total = salaryBase + bonusAmount

        val remainingWorkDays = allWorkDays.count {
            LocalDate.parse(it.date, dateFormatter) > today
        }

        val advanceNet = calculateAdvance(settings, previousMonthWorkDays, hourRate)
        val salary20Net = calculateSalary20(
            settings, workDays, previousMonthWorkDays, previousMonthPoints, hourRate
        )

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
            total = total,
            advance5Amount = advanceNet,
            salary20Amount = salary20Net
        )
    }

    private fun calculateAdvance(
        settings: Settings,
        previousMonthWorkDays: List<ru.dohod.moidohod.data.entity.WorkDay>,
        hourRate: Double
    ): Double {
        val previousYearMonth = currentYearMonth.minusMonths(1)
        val start = previousYearMonth.atDay(16)
        val end = previousYearMonth.atEndOfMonth()
        val workedDays = previousMonthWorkDays.count {
            val date = LocalDate.parse(it.date, dateFormatter)
            it.type == DayType.WORK && date in start..end
        }
        val hours = workedDays * settings.shiftHours
        val gross = hourRate * hours
        val net = gross * (1 - settings.taxRatePercent / 100.0)
        val extrasNet = (settings.carDepreciation + settings.travelCompensation) *
                (1 - settings.taxRatePercent / 100.0)
        return net + extrasNet
    }

    private fun calculateSalary20(
        settings: Settings,
        currentMonthWorkDays: List<ru.dohod.moidohod.data.entity.WorkDay>,
        previousMonthWorkDays: List<ru.dohod.moidohod.data.entity.WorkDay>,
        previousMonthPoints: Double,
        hourRate: Double
    ): Double {
        val currentYearMonth = YearMonth.now()
        val previousYearMonth = currentYearMonth.minusMonths(1)

        val startSalary = currentYearMonth.atDay(1)
        val endSalary = currentYearMonth.atDay(15)
        val salaryWorkedDays = currentMonthWorkDays.count {
            val date = LocalDate.parse(it.date, dateFormatter)
            it.type == DayType.WORK && date in startSalary..endSalary
        }
        val salaryHours = salaryWorkedDays * settings.shiftHours
        val salaryGross = hourRate * salaryHours

        val previousMonthWorkedDaysAll = previousMonthWorkDays.filter { it.type == DayType.WORK }
        // planPointsPrevious — Double, потому что dailyBonusNorm Double
        val planPointsPrevious: Double = previousMonthWorkedDaysAll.size * settings.dailyBonusNorm

        val bonusPercentRaw = if (planPointsPrevious > 0.0) {
            (previousMonthPoints / planPointsPrevious) * 100
        } else 0.0
        val bonusPercent = bonusPercentRaw.coerceIn(0.0, 150.0)

        val previousMonthHours = previousMonthWorkedDaysAll.size * settings.shiftHours
        val previousMonthSalaryGross = hourRate * previousMonthHours

        val bonusAmount = if (bonusPercent >= 60) {
            previousMonthSalaryGross * (bonusPercent / 100.0)
        } else 0.0

        val totalGross = salaryGross + bonusAmount
        return totalGross * (1 - settings.taxRatePercent / 100.0)
    }
}