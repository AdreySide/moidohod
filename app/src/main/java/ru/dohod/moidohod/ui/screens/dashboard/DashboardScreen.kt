package ru.dohod.moidohod.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.dohod.moidohod.MoidohodApp
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            (LocalContext.current.applicationContext as MoidohodApp).settingsRepository,
            (LocalContext.current.applicationContext as MoidohodApp).workDayRepository,
            (LocalContext.current.applicationContext as MoidohodApp).completedTaskRepository
        )
    )
) {
    val data = viewModel.dashboardData
    val isLoading = viewModel.isLoading
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("ru", "RU")) }
    var showBonusDetails by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            BonusCircle(percent = data.bonusPercent, modifier = Modifier.padding(vertical = 16.dp), circleSize = 220f)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showBonusDetails = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "Баллы: ${String.format("%.2f", data.earnedPoints)} / ${String.format("%.2f", data.planPoints)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Осталось рабочих дней: ${data.remainingWorkDays}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (data.bonusAmount > 0) {
                        Text(
                            text = "Премия: ${data.bonusPercent}% (${currencyFormat.format(data.bonusAmount)})",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Премия не начислена (нужно ≥60% плана)",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("К 5-му числу", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(
                            text = currencyFormat.format(data.advance5Amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text("аванс + доплаты", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("К 20-му числу", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = currencyFormat.format(data.salary20Amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text("оклад 1–15 + премия", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }

    if (showBonusDetails) {
        ModalBottomSheet(
            onDismissRequest = { showBonusDetails = false },
            sheetState = sheetState
        ) {
            BonusDetailsContent(
                earnedPoints = data.earnedPoints,
                planPoints = data.planPoints,
                remainingDays = data.remainingWorkDays,
                onClose = { showBonusDetails = false }
            )
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun BonusCircle(percent: Int, modifier: Modifier = Modifier, circleSize: Float = 200f) {
    val animatedPercent by animateFloatAsState(targetValue = percent.toFloat(), label = "bonus_percent")
    Box(modifier = modifier.size(circleSize.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(circleSize.dp)) {
            val canvasSize = this.size
            val strokeWidth = canvasSize.width * 0.08f
            val radius = (canvasSize.width - strokeWidth) / 2
            val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
            drawCircle(color = Color.LightGray.copy(alpha = 0.3f), radius = radius, center = center, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            val fillColor = when {
                percent < 50 -> Color.Green
                percent < 100 -> Color(0xFF8BC34A)
                percent < 130 -> Color(0xFFFFD700)
                else -> Color(0xFFFFA500)
            }
            val brush = Brush.sweepGradient(colors = listOf(fillColor, fillColor.copy(alpha = 0.7f), fillColor), center = center)
            val sweepAngle = (animatedPercent / 150f) * 360f
            drawArc(
                brush = brush,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Text(text = "$percent%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun BonusDetailsContent(
    earnedPoints: Double,
    planPoints: Double,
    remainingDays: Int,
    onClose: () -> Unit
) {
    val plan60 = planPoints * 0.6
    val plan100 = planPoints
    val plan150 = planPoints * 1.5
    val need60 = maxOf(0.0, plan60 - earnedPoints)
    val need100 = maxOf(0.0, plan100 - earnedPoints)
    val need150 = maxOf(0.0, plan150 - earnedPoints)
    val perDay60 = if (remainingDays > 0) need60 / remainingDays else 0.0
    val perDay100 = if (remainingDays > 0) need100 / remainingDays else 0.0
    val perDay150 = if (remainingDays > 0) need150 / remainingDays else 0.0

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Детали премии", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        BonusRequirementRow("60%", need60, perDay60, MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        BonusRequirementRow("100%", need100, perDay100, MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        BonusRequirementRow("150%", need150, perDay150, MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Осталось рабочих дней: $remainingDays", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
    }
}

@Composable
fun BonusRequirementRow(title: String, need: Double, perDay: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("До $title:", fontWeight = FontWeight.Medium)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (need > 0) String.format("%.2f баллов", need) else "✅ достигнуто",
                color = if (need > 0) MaterialTheme.colorScheme.onSurface else color
            )
            if (need > 0) {
                Text(String.format("(%.2f в день)", perDay), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}