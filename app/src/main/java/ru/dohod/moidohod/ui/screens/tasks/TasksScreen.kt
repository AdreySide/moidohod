package ru.dohod.moidohod.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dohod.moidohod.MoidohodApp
import ru.dohod.moidohod.data.entity.TaskType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = viewModel(
        factory = TasksViewModelFactory(
            (LocalContext.current.applicationContext as MoidohodApp).taskTypeRepository,
            (LocalContext.current.applicationContext as MoidohodApp).completedTaskRepository
        )
    )
) {
    val uiState = viewModel.uiState
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Типы заявок", "Выполненные")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Заявки и баллы") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            when (selectedTabIndex) {
                0 -> TaskTypesTab(viewModel, uiState.taskTypes)
                1 -> CompletedTasksTab(viewModel, uiState)
            }
        }
    }

    if (viewModel.showDeleteWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Невозможно удалить тип") },
            text = { Text("Сначала удалите все выполненные заявки этого типа.") },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text("OK") }
            }
        )
    }
}

@Composable
fun TaskTypesTab(viewModel: TasksViewModel, taskTypes: List<TaskType>) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTaskType by remember { mutableStateOf<TaskType?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(taskTypes) { type ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = type.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${String.format("%.2f", type.pointsPerUnit)} балл(ов) за заявку",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row {
                        IconButton(onClick = { editingTaskType = type }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                        }
                        IconButton(onClick = { viewModel.deleteTaskType(type) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить тип заявки")
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditTaskTypeDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, points ->
                viewModel.addTaskType(name, points)
                showAddDialog = false
            }
        )
    }

    editingTaskType?.let { type ->
        AddEditTaskTypeDialog(
            initialName = type.name,
            initialPoints = type.pointsPerUnit,
            onDismiss = { editingTaskType = null },
            onSave = { name, points ->
                viewModel.updateTaskType(type.copy(name = name, pointsPerUnit = points))
                editingTaskType = null
            }
        )
    }
}

@Composable
fun AddEditTaskTypeDialog(
    initialName: String = "",
    initialPoints: Double = 1.0,
    onDismiss: () -> Unit,
    onSave: (name: String, points: Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var points by remember { mutableStateOf(initialPoints.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) "Новый тип заявки" else "Редактировать тип") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = points,
                    onValueChange = { points = it },
                    label = { Text("Баллов за заявку") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val pointsDouble = points.toDoubleOrNull() ?: 1.0
                    if (name.isNotBlank()) onSave(name, pointsDouble)
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun CompletedTasksTab(viewModel: TasksViewModel, uiState: TasksUiState) {
    var showAddDialog by remember { mutableStateOf(false) }
    val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
    val yearMonth = uiState.selectedMonth

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.setMonth(yearMonth.minusMonths(1)) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий месяц")
            }
            Text(
                text = yearMonth.format(monthFormatter).replaceFirstChar { it.titlecase(Locale("ru")) },
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { viewModel.setMonth(yearMonth.plusMonths(1)) }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Следующий месяц")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Всего баллов:", fontWeight = FontWeight.Bold)
                Text(
                    text = String.format("%.2f", uiState.totalPointsForMonth),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.completedTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет выполненных заявок за этот месяц", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                val tasksByDate = uiState.completedTasks.groupBy { it.date }
                tasksByDate.forEach { (date, tasks) ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(
                                    text = date,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                tasks.forEach { task ->
                                    val taskType = uiState.taskTypes.find { it.id == task.taskTypeId }
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${taskType?.name ?: "?"} × ${task.quantity}",
                                                fontWeight = FontWeight.Medium
                                            )
                                            Row {
                                                Text(
                                                    text = "${String.format("%.2f", task.totalPoints)} баллов",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deleteCompletedTask(task) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Удалить",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                        if (task.description.isNotBlank()) {
                                            Text(
                                                text = task.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить выполненную заявку")
            }
        }
    }

    if (showAddDialog) {
        AddCompletedTaskDialog(
            taskTypes = uiState.taskTypes,
            onDismiss = { showAddDialog = false },
            onSave = { date, taskType, quantity, description ->
                viewModel.addCompletedTask(date, taskType, quantity, description)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCompletedTaskDialog(
    taskTypes: List<TaskType>,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, taskType: TaskType, quantity: Int, description: String) -> Unit
) {
    var selectedTaskType by remember { mutableStateOf(taskTypes.firstOrNull()) }
    var quantity by remember { mutableStateOf("1") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить выполненную заявку") },
        text = {
            Column {
                Text("Тип заявки", style = MaterialTheme.typography.labelMedium)
                taskTypes.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().selectable(
                            selected = selectedTaskType == type,
                            onClick = { selectedTaskType = type }
                        )
                    ) {
                        RadioButton(selected = selectedTaskType == type, onClick = null)
                        Text(
                            text = "${type.name} (${String.format("%.2f", type.pointsPerUnit)} балл)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание / Адрес / Номер") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Количество") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Выбрать дату: ${selectedDate.format(dateFormatter)}")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedTaskType != null) {
                        val q = quantity.toIntOrNull() ?: 1
                        onSave(selectedDate, selectedTaskType!!, q, description.trim())
                    }
                },
                enabled = selectedTaskType != null && quantity.toIntOrNull() != null
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}