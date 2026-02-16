package ru.dohod.moidohod.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BonusDetailsContent(
    earnedPoints: Int,
    planPoints: Int,
    remainingDays: Int,
    onClose: () -> Unit
) {
    val plan60 = (planPoints * 0.6).toInt()
    val plan100 = planPoints
    val plan150 = (planPoints * 1.5).toInt()

    val need60 = maxOf(0, plan60 - earnedPoints)
    val need100 = maxOf(0, plan100 - earnedPoints)
    val need150 = maxOf(0, plan150 - earnedPoints)

    val perDay60 = if (remainingDays > 0) need60.toDouble() / remainingDays else 0.0
    val perDay100 = if (remainingDays > 0) need100.toDouble() / remainingDays else 0.0
    val perDay150 = if (remainingDays > 0) need150.toDouble() / remainingDays else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Детали премии",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        BonusRequirementRow(
            title = "60%",
            need = need60,
            perDay = perDay60,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        BonusRequirementRow(
            title = "100%",
            need = need100,
            perDay = perDay100,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        BonusRequirementRow(
            title = "150%",
            need = need150,
            perDay = perDay150,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Осталось рабочих дней: $remainingDays",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("Закрыть")
        }
    }
}

@Composable
fun BonusRequirementRow(
    title: String,
    need: Int,
    perDay: Double,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("До $title:", fontWeight = FontWeight.Medium)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (need > 0) "$need баллов" else "✅ достигнуто",
                color = if (need > 0) MaterialTheme.colorScheme.onSurface else color
            )
            if (need > 0) {
                Text(
                    text = "(${String.format("%.1f", perDay)} в день)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}