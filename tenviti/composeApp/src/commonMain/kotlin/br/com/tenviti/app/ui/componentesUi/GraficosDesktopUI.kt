package br.com.tenviti.app.ui.componentesUi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

@Composable
fun GraficoFaturamentoDesktopUI(
    modifier: Modifier,
    exibirEixoY: Boolean,
    dados: List<Double>,
    labels: List<String>
) {
    // Se a lista estiver vazia (carregando), usa valores zerados para não quebrar
    val valores = if(dados.isEmpty()) listOf(0f,0f,0f,0f,0f,0f,0f) else dados.map { it.toFloat() }
    val dias = labels.ifEmpty { listOf("","","","","","","") }

    val corDaBarra = Color(0xFF790001)
    val corDaGrade = Color.LightGray.copy(alpha = 0.5f)

    val maxValorBruto = valores.maxOrNull() ?: 0f
    // Evita divisão por zero se o max for 0
    val maxValorEixoY = if (maxValorBruto == 0f) 100f else (ceil(maxValorBruto / 100) * 100)
    val numeroDeLinhas = 5

    Row(modifier = modifier.fillMaxWidth().height(250.dp)) {
        if (exibirEixoY) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                for (i in numeroDeLinhas downTo 0) {
                    val valor = maxValorEixoY / numeroDeLinhas * i
                    Text(
                        text = "R$ ${valor.toInt()}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val alturaCanvas = size.height
                val larguraCanvas = size.width

                if (exibirEixoY) {
                    for (i in 0..numeroDeLinhas) {
                        val y = alturaCanvas - (i.toFloat() / numeroDeLinhas) * alturaCanvas
                        drawLine(
                            color = corDaGrade,
                            start = Offset(x = 0f, y = y),
                            end = Offset(x = larguraCanvas, y = y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                val espacamentoEntreBarras = 16.dp.toPx()
                // Evita divisão por zero
                val qtdDias = if (dias.isEmpty()) 1 else dias.size
                val larguraTotalBarras = larguraCanvas - (espacamentoEntreBarras * (qtdDias + 1))
                val larguraBarra = larguraTotalBarras / qtdDias

                valores.forEachIndexed { index, valor ->
                    if (index < dias.size) {
                        val alturaBarra = (valor / maxValorEixoY) * alturaCanvas
                        val x = espacamentoEntreBarras + (index * (larguraBarra + espacamentoEntreBarras))
                        val y = alturaCanvas - alturaBarra

                        drawRect(
                            color = corDaBarra,
                            topLeft = Offset(x = x, y = y),
                            size = Size(width = larguraBarra, height = alturaBarra)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                dias.forEach { dia ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = dia, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun GraficoVendasDesktopUI(
    modifier: Modifier,
    exibirEixoY: Boolean,
    dados: List<Double>,
    labels: List<String>
) {
    val valores = if(dados.isEmpty()) listOf(0f,0f,0f,0f,0f,0f,0f) else dados.map { it.toFloat() }
    val dias = if(labels.isEmpty()) listOf("","","","","","","") else labels

    val corDaLinha = Color(0xFF0D47A1)
    val corDaArea = corDaLinha.copy(alpha = 0.2f)
    val corDaGrade = Color.LightGray.copy(alpha = 0.5f)

    val maxValorBruto = valores.maxOrNull() ?: 0f
    val maxValorEixoY = if (maxValorBruto == 0f) 10f else (ceil(maxValorBruto / 10) * 10)
    val numeroDeLinhas = 5

    Row(modifier = modifier.fillMaxWidth().height(250.dp)) {
        if (exibirEixoY) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                for (i in numeroDeLinhas downTo 0) {
                    val valor = maxValorEixoY / numeroDeLinhas * i
                    Text(
                        text = "${valor.toInt()}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val alturaCanvas = size.height
                val larguraCanvas = size.width
                val qtdDias = if (dias.isEmpty()) 1 else dias.size
                val larguraSlotPx = larguraCanvas / qtdDias

                if (exibirEixoY) {
                    for (i in 0..numeroDeLinhas) {
                        val y = alturaCanvas - (i.toFloat() / numeroDeLinhas) * alturaCanvas
                        drawLine(
                            color = corDaGrade,
                            start = Offset(x = 0f, y = y),
                            end = Offset(x = larguraCanvas, y = y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                val caminhoDaArea = Path().apply { moveTo(0f, alturaCanvas) }
                val caminhoDaLinha = Path()

                valores.forEachIndexed { index, valor ->
                    if (index < qtdDias) {
                        val x = (index * larguraSlotPx) + (larguraSlotPx / 2f)
                        val y = alturaCanvas - (valor / maxValorEixoY) * alturaCanvas

                        if (index == 0) {
                            caminhoDaLinha.moveTo(x, y)
                        } else {
                            caminhoDaLinha.lineTo(x, y)
                        }
                        caminhoDaArea.lineTo(x, y)
                    }
                }

                caminhoDaArea.lineTo(larguraCanvas, alturaCanvas)
                caminhoDaArea.close()

                drawPath(path = caminhoDaArea, color = corDaArea)
                drawPath(path = caminhoDaLinha, color = corDaLinha, style = Stroke(width = 2.dp.toPx()))

                valores.forEachIndexed { index, valor ->
                    if (index < qtdDias) {
                        val x = (index * larguraSlotPx) + (larguraSlotPx / 2f)
                        val y = alturaCanvas - (valor / maxValorEixoY) * alturaCanvas
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = corDaLinha, radius = 2.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                dias.forEach { dia ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = dia, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}