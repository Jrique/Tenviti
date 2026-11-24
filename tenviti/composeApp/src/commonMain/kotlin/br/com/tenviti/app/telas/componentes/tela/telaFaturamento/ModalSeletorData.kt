package br.com.tenviti.app.telas.componentes.tela.telaFaturamento

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.time.ExperimentalTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime as toLocalDateTimeKx


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalSeletorData(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    // Calcula o "hoje" em milissegundos, no fuso horário local
    val hojeMillis = remember {
        val tz = TimeZone.currentSystemDefault()
        val hojeDate = Clock.System.now().toLocalDateTime(tz).date
        val hojeStart = hojeDate.atStartOfDayIn(tz)
        hojeStart.toEpochMilliseconds()
    }

    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = hojeMillis,
        initialSelectedEndDateMillis = hojeMillis

    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF8F6),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Selecione o Período",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF790001),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                DateRangePicker(
                    state = datePickerState,
                    modifier = Modifier.weight(1f),
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = Color(0xFF790001),
                        todayDateBorderColor = Color(0xFF790001),
                        dayContentColor = Color.Black
                    ),
                    title = {},
                    headline = {}
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color(0xFF790001))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val inicioMillis = datePickerState.selectedStartDateMillis
                            val fimMillis = datePickerState.selectedEndDateMillis

                            if (inicioMillis != null && fimMillis != null) {
                                val inicioStr = converterMillisParaData(inicioMillis)
                                val fimStr = converterMillisParaData(fimMillis)
                                onConfirm(inicioStr, fimStr)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF790001)),
                        enabled = datePickerState.selectedStartDateMillis != null &&
                                datePickerState.selectedEndDateMillis != null
                    ) {
                        Text("Filtrar")
                    }
                }
            }
        }
    }
}

fun converterMillisParaData(millis: Long): String {
    if (millis <= 0) return ""

    val instant = Instant.fromEpochMilliseconds(millis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val date = localDateTime.date

    val year = date.year
    val month = date.monthNumber
    val day = date.dayOfMonth

    return buildString {
        append(year)
        append("-")
        append(month.toString().padStart(2, '0'))
        append("-")
        append(day.toString().padStart(2, '0'))
    }
}
