package ru.dohod.moidohod.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.dohod.moidohod.data.entity.CompletedTask

@Dao
interface CompletedTaskDao {
    @Query("SELECT * FROM completed_tasks WHERE date LIKE :yearMonth || '%' ORDER BY date DESC, id DESC")
    fun getTasksForMonth(yearMonth: String): Flow<List<CompletedTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completedTask: CompletedTask)

    @Delete
    suspend fun delete(completedTask: CompletedTask)

    @Query("SELECT SUM(totalPoints) FROM completed_tasks WHERE date LIKE :yearMonth || '%'")
    suspend fun getTotalPointsForMonth(yearMonth: String): Double?
}