package ru.dohod.moidohod.ui.screens.tasks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.dohod.moidohod.data.entity.CompletedTask
import ru.dohod.moidohod.data.entity.TaskType
import ru.dohod.moidohod.data.repository.CompletedTaskRepository
import ru.dohod.moidohod.data.repository.TaskTypeRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class TasksUiState(
    val taskTypes: List<TaskType> = emptyList(),
    val completedTasks: List<CompletedTask> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val isLoading: Boolean = false,
    val totalPointsForMonth: Int = 0
)

class TasksViewModel(
    private val taskTypeRepository: TaskTypeRepository,
    private val completedTaskRepository: CompletedTaskRepository
) : ViewModel() {

    var uiState by mutableStateOf(TasksUiState())
        private set

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadData()
    }

    fun setMonth(yearMonth: YearMonth) {
        uiState = uiState.copy(selectedMonth = yearMonth)
        loadCompletedTasks()
    }

    private fun loadData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            // Однократно загружаем типы заявок (не блокируем корутину)
            val types = taskTypeRepository.getAllTaskTypes().first()
            uiState = uiState.copy(taskTypes = types)
            // Загружаем выполненные заявки
            loadCompletedTasks()
            uiState = uiState.copy(isLoading = false)
        }
    }

    // Принудительная перезагрузка типов (после добавления/редактирования/удаления)
    private fun loadTaskTypes() {
        viewModelScope.launch {
            val types = taskTypeRepository.getAllTaskTypes().first()
            uiState = uiState.copy(taskTypes = types)
        }
    }

    private fun loadCompletedTasks() {
        viewModelScope.launch {
            val monthStr = uiState.selectedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            completedTaskRepository.getTasksForMonthFlow(monthStr).collect { tasks ->
                val total = tasks.sumOf { it.totalPoints }
                uiState = uiState.copy(
                    completedTasks = tasks,
                    totalPointsForMonth = total
                )
            }
        }
    }

    fun addTaskType(name: String, pointsPerUnit: Int) {
        viewModelScope.launch {
            val newType = TaskType(
                name = name,
                pointsPerUnit = pointsPerUnit,
                isActive = true
            )
            taskTypeRepository.insert(newType)
            loadTaskTypes() // обновляем список типов
        }
    }

    fun updateTaskType(taskType: TaskType) {
        viewModelScope.launch {
            taskTypeRepository.update(taskType)
            loadTaskTypes() // обновляем список типов
        }
    }

    fun deleteTaskType(taskType: TaskType) {
        viewModelScope.launch {
            // Проверяем, используется ли этот тип в выполненных заявках (за текущий месяц — достаточно)
            val monthStr = uiState.selectedMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val completed = completedTaskRepository.getTasksForMonthFlow(monthStr).first()
            val isUsed = completed.any { it.taskTypeId == taskType.id }
            if (isUsed) {
                // TODO: показать сообщение, что нельзя удалить (можно добавить Snackbar)
                return@launch
            }
            taskTypeRepository.delete(taskType)
            loadTaskTypes() // обновляем список типов
        }
    }

    fun addCompletedTask(date: LocalDate, taskType: TaskType, quantity: Int, description: String = "") {
        viewModelScope.launch {
            val completedTask = CompletedTask(
                date = date.format(dateFormatter),
                taskTypeId = taskType.id,
                quantity = quantity,
                totalPoints = taskType.pointsPerUnit * quantity,
                description = description
            )
            completedTaskRepository.insert(completedTask)
            // Принудительно перезагружаем список заявок за текущий месяц
            loadCompletedTasks()
        }
    }

    fun deleteCompletedTask(completedTask: CompletedTask) {
        viewModelScope.launch {
            completedTaskRepository.delete(completedTask)
            loadCompletedTasks()
        }
    }
}