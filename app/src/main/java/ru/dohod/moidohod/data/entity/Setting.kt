package ru.dohod.moidohod.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WorkSchedule {
    FIVE_TWO,
    TWO_TWO
}

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val salary: Double = 0.0,
    val yearNormHours: Int = 1974,
    val taxRatePercent: Int = 13,
    val schedule: WorkSchedule = WorkSchedule.FIVE_TWO,
    val carDepreciation: Double = 0.0,
    val travelCompensation: Double = 0.0
) {
    val shiftHours: Int
        get() = when (schedule) {
            WorkSchedule.FIVE_TWO -> 8
            WorkSchedule.TWO_TWO -> 11
        }

    val bonusHours: Int
        get() = when (schedule) {
            WorkSchedule.FIVE_TWO -> 8
            WorkSchedule.TWO_TWO -> 12
        }

    val dailyBonusNorm: Double
        get() = bonusHours * 0.9
}