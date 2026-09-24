package ru.dohod.moidohod.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dohod.moidohod.MoidohodApp
import ru.dohod.moidohod.data.entity.WorkSchedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            (LocalContext.current.applicationContext as MoidohodApp).settingsRepository
        )
    )
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saveCompleted by viewModel.saveCompleted.collectAsState()

    var salaryInput by remember { mutableStateOf(settings?.salary?.toString() ?: "") }
    var yearNormInput by remember { mutableStateOf(settings?.yearNormHours?.toString() ?: "") }
    var taxRateInput by remember { mutableStateOf(settings?.taxRatePercent?.toString() ?: "13") }
    var selectedSchedule by remember { mutableStateOf(settings?.schedule ?: WorkSchedule.FIVE_TWO) }
    var carDepreciationInput by remember { mutableStateOf(settings?.carDepreciation?.toString() ?: "") }
    var travelCompensationInput by remember { mutableStateOf(settings?.travelCompensation?.toString() ?: "") }

    LaunchedEffect(settings) {
        settings?.let {
            salaryInput = it.salary.toString()
            yearNormInput = it.yearNormHours.toString()
            taxRateInput = it.taxRatePercent.toString()
            selectedSchedule = it.schedule
            carDepreciationInput = it.carDepreciation.toString()
            travelCompensationInput = it.travelCompensation.toString()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                OutlinedTextField(
                    value = salaryInput,
                    onValueChange = { salaryInput = it },
                    label = { Text("Оклад (₽)") },
                    placeholder = { Text("Введите оклад") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = yearNormInput,
                    onValueChange = { yearNormInput = it },
                    label = { Text("Годовая норма часов") },
                    placeholder = { Text("Введите норму (например, 1974)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = taxRateInput,
                    onValueChange = { taxRateInput = it },
                    label = { Text("Ставка налога (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("График работы", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.weight(1f).selectable(
                            selected = selectedSchedule == WorkSchedule.FIVE_TWO,
                            onClick = { selectedSchedule = WorkSchedule.FIVE_TWO }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSchedule == WorkSchedule.FIVE_TWO,
                            onClick = null
                        )
                        Text("5/2 (8 часов)")
                    }
                    Row(
                        Modifier.weight(1f).selectable(
                            selected = selectedSchedule == WorkSchedule.TWO_TWO,
                            onClick = { selectedSchedule = WorkSchedule.TWO_TWO }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSchedule == WorkSchedule.TWO_TWO,
                            onClick = null
                        )
                        Text("2/2 (11 часов)")
                    }
                }

                OutlinedTextField(
                    value = carDepreciationInput,
                    onValueChange = { carDepreciationInput = it },
                    label = { Text("Амортизация авто (₽)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = travelCompensationInput,
                    onValueChange = { travelCompensationInput = it },
                    label = { Text("Разъездной характер (₽)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.saveSettings(
                            salary = salaryInput.toDoubleOrNull() ?: 0.0,
                            yearNormHours = yearNormInput.toIntOrNull() ?: 1974,
                            taxRatePercent = taxRateInput.toIntOrNull() ?: 13,
                            schedule = selectedSchedule,
                            carDepreciation = carDepreciationInput.toDoubleOrNull() ?: 0.0,
                            travelCompensation = travelCompensationInput.toDoubleOrNull() ?: 0.0
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Сохранить")
                }

                if (saveCompleted) {
                    Text(
                        text = "Сохранено!",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}