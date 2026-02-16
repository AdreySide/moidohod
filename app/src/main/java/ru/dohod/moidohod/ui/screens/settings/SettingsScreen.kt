package ru.dohod.moidohod.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dohod.moidohod.MoidohodApp

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

    var salaryInput by remember { mutableStateOf(settings?.salary?.toString() ?: "74500") }
    var yearNormInput by remember { mutableStateOf(settings?.yearNormHours?.toString() ?: "1974") }
    var dailyBonusInput by remember { mutableStateOf(settings?.dailyBonusNorm?.toString() ?: "7.2") }
    var taxRateInput by remember { mutableStateOf(settings?.taxRatePercent?.toString() ?: "13") }

    LaunchedEffect(settings) {
        settings?.let {
            salaryInput = it.salary.toString()
            yearNormInput = it.yearNormHours.toString()
            dailyBonusInput = it.dailyBonusNorm.toString()
            taxRateInput = it.taxRatePercent.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Настройки") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = yearNormInput,
                    onValueChange = { yearNormInput = it },
                    label = { Text("Годовая норма часов") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dailyBonusInput,
                    onValueChange = { dailyBonusInput = it },
                    label = { Text("Дневная норма баллов") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                Button(
                    onClick = {
                        viewModel.saveSettings(
                            salary = salaryInput.toDoubleOrNull() ?: 74500.0,
                            yearNormHours = yearNormInput.toIntOrNull() ?: 1974,
                            dailyBonusNorm = dailyBonusInput.toDoubleOrNull() ?: 7.2,
                            taxRatePercent = taxRateInput.toIntOrNull() ?: 13
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