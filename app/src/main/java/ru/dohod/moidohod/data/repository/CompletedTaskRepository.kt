package ru.dohod.moidohod.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.dohod.moidohod.data.dao.CompletedTaskDao
import ru.dohod.moidohod.data.entity.CompletedTask

class CompletedTaskRepository(private val completedTaskDao: CompletedTaskDao) {

    fun getTasksForMonthFlow(yearMonth: String): Flow<List<CompletedTask>> =
        completedTaskDao.getTasksForMonth(yearMonth)

    fun getTotalPointsForMonthFlow(yearMonth: String): Flow<Double> = flow {
        completedTaskDao.getTasksForMonth(yearMonth).collect { tasks ->
            emit(tasks.sumOf { it.totalPoints })
        }
    }

    suspend fun insert(completedTask: CompletedTask) =
        completedTaskDao.insert(completedTask)

    suspend fun delete(completedTask: CompletedTask) =
        completedTaskDao.delete(completedTask)

    suspend fun getTotalPointsForMonth(yearMonth: String): Double =
        completedTaskDao.getTotalPointsForMonth(yearMonth) ?: 0.0
}