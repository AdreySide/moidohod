package ru.dohod.moidohod.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class DayType {
    WORK,
    DAY_OFF,
    SICK_LEAVE,
    VACATION
}

@Entity(tableName = "work_days")
data class WorkDay(
    @PrimaryKey val date: String,
    val type: DayType,
    val hours: Int = if (type == DayType.WORK) 8 else 0
)