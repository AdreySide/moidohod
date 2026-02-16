package ru.dohod.moidohod.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.dohod.moidohod.data.entity.TaskType

@Dao
interface TaskTypeDao {
    @Query("SELECT * FROM task_types ORDER BY id ASC")
    fun getAllTaskTypes(): Flow<List<TaskType>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taskType: TaskType)

    @Update
    suspend fun update(taskType: TaskType)

    @Delete
    suspend fun delete(taskType: TaskType)
}