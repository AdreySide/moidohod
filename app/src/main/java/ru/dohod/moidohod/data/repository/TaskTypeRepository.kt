package ru.dohod.moidohod.data.repository

import kotlinx.coroutines.flow.Flow
import ru.dohod.moidohod.data.dao.TaskTypeDao
import ru.dohod.moidohod.data.entity.TaskType

class TaskTypeRepository(private val taskTypeDao: TaskTypeDao) {
    fun getAllTaskTypes(): Flow<List<TaskType>> = taskTypeDao.getAllTaskTypes()
    suspend fun insert(taskType: TaskType) = taskTypeDao.insert(taskType)
    suspend fun update(taskType: TaskType) = taskTypeDao.update(taskType)
    suspend fun delete(taskType: TaskType) = taskTypeDao.delete(taskType)
}