package ru.dohod.moidohod.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_types")
data class TaskType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pointsPerUnit: Double,
    val isActive: Boolean = true
)