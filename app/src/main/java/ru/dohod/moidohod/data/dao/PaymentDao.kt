package ru.dohod.moidohod.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.dohod.moidohod.data.entity.Payment

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE date LIKE :yearMonth || '%' ORDER BY date DESC")
    fun getPaymentsForMonth(yearMonth: String): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment)

    @Update
    suspend fun update(payment: Payment)

    @Delete
    suspend fun delete(payment: Payment)
}