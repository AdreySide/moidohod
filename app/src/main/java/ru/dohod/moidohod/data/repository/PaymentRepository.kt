package ru.dohod.moidohod.data.repository

import kotlinx.coroutines.flow.Flow
import ru.dohod.moidohod.data.dao.PaymentDao
import ru.dohod.moidohod.data.entity.Payment

class PaymentRepository(private val paymentDao: PaymentDao) {
    fun getAllPayments(): Flow<List<Payment>> = paymentDao.getAllPayments()
    fun getPaymentsForMonth(yearMonth: String): Flow<List<Payment>> = paymentDao.getPaymentsForMonth(yearMonth)
    suspend fun insert(payment: Payment) = paymentDao.insert(payment)
    suspend fun update(payment: Payment) = paymentDao.update(payment)
    suspend fun delete(payment: Payment) = paymentDao.delete(payment)
}
