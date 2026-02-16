package ru.dohod.moidohod.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_tasks")
data class CompletedTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val taskTypeId: Int,
    val quantity: Int,
    val totalPoints: Int,
    val description: String = ""
)