package ru.dohod.moidohod.ui.screens.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.dohod.moidohod.data.entity.DayType
import ru.dohod.moidohod.data.entity.WorkDay
import ru.dohod.moidohod.data.repository.SettingsRepository
import ru.dohod.moidohod.data.repository.WorkDayRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarViewModel(
    private val workDayRepository: WorkDayRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    var currentMonth by mutableStateOf(YearMonth.now())
        private set

    var daysInMonth by mutableStateOf<List<WorkDay>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {

            viewModelScope.launch {
                val yearMonthStr = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val count = workDayRepository.getDaysForMonthSync(yearMonthStr).size
                println("🔥 INIT: загружено $count дней")
            }
            loadMonth(currentMonth)

    }

    fun setMonth(yearMonth: YearMonth) {
        currentMonth = yearMonth
        loadMonth(yearMonth)
    }

    private fun loadMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            isLoading = true
            val yearMonthStr = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            daysInMonth = workDayRepository.getDaysForMonthSync(yearMonthStr)
            isLoading = false
        }
    }

    fun setDayType(date: LocalDate, type: DayType) {
        viewModelScope.launch {
            try {
                val hours = if (type == DayType.WORK) 8 else 0
                val workDay = WorkDay(
                    date = date.format(dateFormatter),
                    type = type,
                    hours = hours
                )
                workDayRepository.saveDay(workDay)
                println("🔥 saveDay успешно: ${workDay.date}")
                // обновление списка
                val index = daysInMonth.indexOfFirst { it.date == workDay.date }
                if (index != -1) {
                    daysInMonth = daysInMonth.toMutableList().apply { set(index, workDay) }
                } else {
                    daysInMonth = daysInMonth + workDay
                }
            } catch (e: Exception) {
                println("🔥 saveDay ошибка: ${e.message}")
            }
        }
    }

    fun getDayType(date: LocalDate): DayType? {
        val dateStr = date.format(dateFormatter)
        return daysInMonth.find { it.date == dateStr }?.type
    }
}