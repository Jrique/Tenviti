package br.com.tenviti.app.ui.componentesUi

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
fun GraficoFaturamentoAndroidUI(
    modifier: Modifier,
    exibirEixoY: Boolean,
    dados: List<Double>,
    labels: List<String>
) {
    val valores = if(dados.isEmpty()) listOf(0f) else dados.map { it.toFloat() }
    val dias = labels.ifEmpty { listOf("") }

    val corDaBarra = TenvitiTema.cores.base
    val corDaGrade = Color.LightGray.copy(alpha = 0.5f)
    val larguraBarra = 40.dp
    val espacamentoEntreBarras = 20.dp

    val maxValorBruto = valores.maxOrNull() ?: 0f
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
                        color = TenvitiTema.cores.textoSuave,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        val scrollState = rememberScrollState()

        Column(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((larguraBarra + espacamentoEntreBarras) * dias.size)
            ) {
                val alturaCanvas = size.height
                val larguraBarraPx = larguraBarra.toPx()
                val espacamentoPx = espacamentoEntreBarras.toPx()

                if (exibirEixoY) {
                    for (i in 0..numeroDeLinhas) {
                        val y = alturaCanvas - (i.toFloat() / numeroDeLinhas) * alturaCanvas
                        drawLine(
                            color = corDaGrade,
                            start = Offset(x = 0f, y = y),
                            end = Offset(x = size.width, y = y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                valores.forEachIndexed { index, valor ->
                    if (index < dias.size) {
                        val alturaBarra = (valor / maxValorEixoY) * alturaCanvas
                        val x = (espacamentoPx / 2) + (index * (larguraBarraPx + espacamentoPx))
                        val y = alturaCanvas - alturaBarra

                        drawRect(
                            color = corDaBarra,
                            topLeft = Offset(x = x, y = y),
                            size = Size(width = larguraBarraPx, height = alturaBarra)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.width((larguraBarra + espacamentoEntreBarras) * dias.size),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dias.forEach { dia ->
                    Box(modifier = Modifier.width(larguraBarra + espacamentoEntreBarras), contentAlignment = Alignment.Center) {
                        Text(text = dia, fontSize = 12.sp, color = TenvitiTema.cores.textoSuave)
                    }
                }
            }
        }
    }
}

@Composable
fun GraficoVendasAndroidUI(
    modifier: Modifier,
    exibirEixoY: Boolean,
    dados: List<Double>,
    labels: List<String>
) {
    val valores = if(dados.isEmpty()) listOf(0f) else dados.map { it.toFloat() }
    val dias = labels.ifEmpty { listOf("") }

    val corDaLinha = TenvitiTema.cores.info.texto
    val corDaArea = corDaLinha.copy(alpha = 0.2f)
    val corDaGrade = Color.LightGray.copy(alpha = 0.5f)
    val larguraSlot = 60.dp

    val maxValorBruto = valores.maxOrNull() ?: 0f
    val maxValorEixoY = if (maxValorBruto == 0f) 10f else (ceil(maxValorBruto / 10) * 10)
    val numeroDeLinhas = 5

    val scrollState = rememberScrollState()

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
                        color = TenvitiTema.cores.textoSuave,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
            val larguraTotalCanvas = larguraSlot * dias.size
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(larguraTotalCanvas)
            ) {
                val alturaCanvas = size.height
                val larguraSlotPx = larguraSlot.toPx()

                if (exibirEixoY) {
                    for (i in 0..numeroDeLinhas) {
                        val y = alturaCanvas - (i.toFloat() / numeroDeLinhas) * alturaCanvas
                        drawLine(
                            color = corDaGrade,
                            start = Offset(x = 0f, y = y),
                            end = Offset(x = size.width, y = y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                val caminhoDaArea = Path().apply { moveTo(0f, alturaCanvas) }
                val caminhoDaLinha = Path()

                valores.forEachIndexed { index, valor ->
                    if (index < dias.size) {
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

                caminhoDaArea.lineTo(size.width, alturaCanvas)
                caminhoDaArea.close()

                drawPath(path = caminhoDaArea, color = corDaArea)
                drawPath(path = caminhoDaLinha, color = corDaLinha, style = Stroke(width = 2.dp.toPx()))

                valores.forEachIndexed { index, valor ->
                    if (index < dias.size) {
                        val x = (index * larguraSlotPx) + (larguraSlotPx / 2f)
                        val y = alturaCanvas - (valor / maxValorEixoY) * alturaCanvas
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = corDaLinha, radius = 2.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.width(larguraSlot * dias.size)) {
                dias.forEach { dia ->
                    Box(modifier = Modifier.width(larguraSlot), contentAlignment = Alignment.Center) {
                        Text(text = dia, fontSize = 12.sp, color = TenvitiTema.cores.textoSuave)
                    }
                }
            }
        }
    }
}