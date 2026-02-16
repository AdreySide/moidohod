package ru.dohod.moidohod.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.dohod.moidohod.data.entity.WorkDay

@Dao
interface WorkDayDao {

    // Flow для автоматического обновления (подписка)
    @Query("SELECT * FROM work_days WHERE date LIKE :yearMonth || '%'")
    fun getDaysForMonthFlow(yearMonth: String): Flow<List<WorkDay>>

    // Suspend для разового запроса
    @Query("SELECT * FROM work_days WHERE date LIKE :yearMonth || '%'")
    suspend fun getDaysForMonthSync(yearMonth: String): List<WorkDay>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workDay: WorkDay)

    @Update
    suspend fun update(workDay: WorkDay)

    @Delete
    suspend fun delete(workDay: WorkDay)

    @Query("SELECT * FROM work_days WHERE date = :date")
    suspend fun getDay(date: String): WorkDay?
}