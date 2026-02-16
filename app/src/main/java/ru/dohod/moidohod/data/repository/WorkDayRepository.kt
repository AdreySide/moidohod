package ru.dohod.moidohod.data.repository

import kotlinx.coroutines.flow.Flow
import ru.dohod.moidohod.data.dao.WorkDayDao
import ru.dohod.moidohod.data.entity.WorkDay

class WorkDayRepository(private val workDayDao: WorkDayDao) {

    // Flow для автоматического обновления — используется в DashboardViewModel
    fun getDaysForMonthFlow(yearMonth: String): Flow<List<WorkDay>> =
        workDayDao.getDaysForMonthFlow(yearMonth)

    // Suspend для разового запроса — используется в CalendarViewModel
    suspend fun getDaysForMonthSync(yearMonth: String): List<WorkDay> =
        workDayDao.getDaysForMonthSync(yearMonth)

    suspend fun saveDay(workDay: WorkDay) {
        workDayDao.insert(workDay)
    }

    suspend fun getDay(date: String): WorkDay? = workDayDao.getDay(date)
}