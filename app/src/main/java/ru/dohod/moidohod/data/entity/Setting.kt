package ru.dohod.moidohod.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val salary: Double = 74500.0,
    val yearNormHours: Int = 1974,
    val dailyBonusNorm: Double = 7.2,
    val taxRatePercent: Int = 13,
    val shiftHours: Int = 8
)