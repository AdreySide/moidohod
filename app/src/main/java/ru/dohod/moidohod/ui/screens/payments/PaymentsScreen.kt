package ru.dohod.moidohod.ui.screens.payments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dohod.moidohod.MoidohodApp
import ru.dohod.moidohod.data.entity.PaymentType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    viewModel: PaymentsViewModel = viewModel(
        factory = PaymentsViewModelFactory(
            (LocalContext.current.applicationContext as MoidohodApp).paymentRepository,
            (LocalContext.current.applicationContext as MoidohodApp).settingsRepository,
            (LocalContext.current.applicationContext as MoidohodApp).workDayRepository,
            (LocalContext.current.applicationContext as MoidohodApp).completedTaskRepository
        )
    )
) {
    val uiState = viewModel.uiState
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Ожидаемые", "История")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Выплаты") }) },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(onClick = { showAddPaymentDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить выплату")
                }
            }
        }
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
                0 -> UpcomingPaymentsTab(
                    upcoming = uiState.upcomingPayments,
                    onMarkReceived = { payment ->
                        val type = if (payment.id.startsWith("advance"))
                            PaymentType.ADVANCE
                        else
                            PaymentType.SALARY
                        viewModel.markPaymentAsReceived(
                            paymentType = type,
                            date = payment.date,
                            amount = payment.amount,
                            description = payment.description
                        )
                    }
                )
                1 -> PaymentHistoryTab(uiState.paymentHistory, viewModel)
            }
        }
    }

    if (showAddPaymentDialog) {
        AddManualPaymentDialog(
            onDismiss = { showAddPaymentDialog = false },
            onSave = { date, amount, description ->
                viewModel.addManualPayment(date, amount, description)
                showAddPaymentDialog = false
            }
        )
    }
}

@Composable
fun UpcomingPaymentsTab(
    upcoming: List<UpcomingPayment>,
    onMarkReceived: (UpcomingPayment) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("ru", "RU")) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru")) }

    if (upcoming.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет предстоящих выплат",
                color = MaterialTheme.colorScheme.outline
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(upcoming) { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (payment.isPast)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = payment.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = payment.date.format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (payment.description.isNotBlank()) {
                            Text(
                                text = payment.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currencyFormat.format(payment.amount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (payment.isPast) {
                                Button(onClick = { onMarkReceived(payment) }) {
                                    Text("Получено")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentHistoryTab(
    history: List<PaymentHistoryGroup>,
    viewModel: PaymentsViewModel
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("ru", "RU")) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale("ru")) }

    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет выплат",
                color = MaterialTheme.colorScheme.outline
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(history) { group ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.month.format(monthFormatter)
                                    .replaceFirstChar { it.titlecase(Locale("ru")) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currencyFormat.format(group.total),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        group.payments.forEach { payment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = when (payment.type) {
                                            PaymentType.ADVANCE -> "Аванс"
                                            PaymentType.SALARY -> "Зарплата"
                                            PaymentType.MANUAL -> "Ручная выплата"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${LocalDate.parse(payment.date).format(dateFormatter)}" +
                                                if (payment.description.isNotBlank())
                                                    " • ${payment.description}"
                                                else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currencyFormat.format(payment.amount),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (payment.type == PaymentType.MANUAL) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.deletePayment(payment) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Удалить",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                            if (payment != group.payments.last()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualPaymentDialog(
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, amount: Double, description: String) -> Unit
) {
    var dateText by remember {
        mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val isFormValid = amountText.toDoubleOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить ручную выплату") },
        text = {
            Column {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Дата (ГГГГ-ММ-ДД)") },
                    placeholder = { Text(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) },
                    singleLine = true,
                    isError = runCatching { LocalDate.parse(dateText) }.isFailure,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма (₽)") },
                    placeholder = { Text("15000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountText.isNotBlank() && (amountText.toDoubleOrNull()?.let { it <= 0 } ?: true),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    placeholder = { Text("Например: Отпускные, Больничный") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val date = LocalDate.parse(dateText)
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onSave(date, amount, description.trim())
                },
                enabled = isFormValid
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}