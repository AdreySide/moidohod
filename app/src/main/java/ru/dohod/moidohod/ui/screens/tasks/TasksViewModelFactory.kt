package ru.dohod.moidohod.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.dohod.moidohod.data.repository.CompletedTaskRepository
import ru.dohod.moidohod.data.repository.TaskTypeRepository

class TasksViewModelFactory(
    private val taskTypeRepository: TaskTypeRepository,
    private val completedTaskRepository: CompletedTaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(taskTypeRepository, completedTaskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}