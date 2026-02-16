package ru.dohod.moidohod.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dohod.moidohod.MoidohodApp
import ru.dohod.moidohod.data.entity.DayType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

data class CalendarDay(
    val id: Int,               // уникальный идентификатор для ключа
    val date: LocalDate,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModelFactory(
            (LocalContext.current.applicationContext as MoidohodApp).database.workDayDao(),
            (LocalContext.current.applicationContext as MoidohodApp).settingsRepository
        )
    )
) {
    val currentMonth = viewModel.currentMonth
    val daysInMonth = viewModel.daysInMonth
    val isLoading = viewModel.isLoading

    var selectedType by remember { mutableStateOf(DayType.WORK) }

    val days = remember(currentMonth) {
        getDaysForMonth(currentMonth)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.setMonth(currentMonth.minusMonths(1)) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущий месяц")
                        }
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))),
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewModel.setMonth(currentMonth.plusMonths(1)) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Следующий месяц")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Заголовки дней недели
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                    dayNames.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (day == "Сб" || day == "Вс")
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Сетка дней
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(days, key = { it.id }) { day ->
                        if (!day.isCurrentMonth) {
                            // Пустая ячейка для неактивных дней
                            Box(modifier = Modifier.aspectRatio(1f).padding(2.dp))
                        } else {
                            DayCell(
                                date = day.date,
                                dayOfMonth = day.dayOfMonth,
                                dayType = viewModel.getDayType(day.date),
                                onClick = { viewModel.setDayType(day.date, selectedType) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Панель выбора типа дня
                TypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )
            }
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    dayOfMonth: Int,
    dayType: DayType?,
    onClick: () -> Unit
) {
    val backgroundColor = when (dayType) {
        DayType.WORK -> Color(0xFF4CAF50).copy(alpha = 0.3f)
        DayType.DAY_OFF -> Color.Gray.copy(alpha = 0.3f)
        DayType.SICK_LEAVE -> Color(0xFFF44336).copy(alpha = 0.3f)
        DayType.VACATION -> Color(0xFF2196F3).copy(alpha = 0.3f)
        null -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = dayOfMonth.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TypeSelector(
    selectedType: DayType,
    onTypeSelected: (DayType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TypeRadioButton(
            type = DayType.WORK,
            label = "Рабочий",
            color = Color(0xFF4CAF50),
            selected = selectedType == DayType.WORK,
            onClick = { onTypeSelected(DayType.WORK) }
        )
        TypeRadioButton(
            type = DayType.DAY_OFF,
            label = "Выходной",
            color = Color.Gray,
            selected = selectedType == DayType.DAY_OFF,
            onClick = { onTypeSelected(DayType.DAY_OFF) }
        )
        TypeRadioButton(
            type = DayType.SICK_LEAVE,
            label = "Больничный",
            color = Color(0xFFF44336),
            selected = selectedType == DayType.SICK_LEAVE,
            onClick = { onTypeSelected(DayType.SICK_LEAVE) }
        )
        TypeRadioButton(
            type = DayType.VACATION,
            label = "Отпуск",
            color = Color(0xFF2196F3),
            selected = selectedType == DayType.VACATION,
            onClick = { onTypeSelected(DayType.VACATION) }
        )
    }
}

@Composable
fun TypeRadioButton(
    type: DayType,
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(if (selected) color else color.copy(alpha = 0.3f), shape = CircleShape)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getDaysForMonth(yearMonth: YearMonth): List<CalendarDay> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val daysInMonth = yearMonth.lengthOfMonth()

    val days = mutableListOf<CalendarDay>()
    var id = 0

    // Пустые ячейки перед первым днём месяца
    for (i in 1 until firstDayOfWeek) {
        days.add(CalendarDay(id++, LocalDate.MIN, -1, false))
    }

    // Дни текущего месяца
    for (day in 1..daysInMonth) {
        val date = yearMonth.atDay(day)
        days.add(CalendarDay(id++, date, day, true))
    }

    // Заполняем оставшиеся ячейки до конца сетки (42 ячейки для 6 рядов)
    val remaining = 42 - days.size
    for (i in 0 until remaining) {
        days.add(CalendarDay(id++, LocalDate.MAX, -1, false))
    }

    return days
}