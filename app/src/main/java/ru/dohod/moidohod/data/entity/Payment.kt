package ru.dohod.moidohod.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class PaymentType {
    ADVANCE,          // аванс (5-е число)
    SALARY,           // зарплата (20-е число)
    MANUAL            // ручное добавление (отпускные, больничные и т.п.)
}

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,               // дата получения (LocalDate в ISO)
    val type: PaymentType,
    val amount: Double,            // сумма **чистыми** (после вычета налога)
    val description: String = "",  // для ручных выплат – назначение
    val periodStart: String? = null, // начало периода, за который выплата (ISO)
    val periodEnd: String? = null,   // конец периода (ISO)
    val isActual: Boolean = true   // false = прогноз, true = фактическая
)