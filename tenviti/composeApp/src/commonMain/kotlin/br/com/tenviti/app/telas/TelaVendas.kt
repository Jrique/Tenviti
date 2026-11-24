package br.com.tenviti.app.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.tenviti.app.getPlatform
import br.com.tenviti.app.telas.componentes.tela.telaFaturamento.ModalSeletorData
import br.com.tenviti.app.telas.componentes.tela.telaVendas.ListaPedidosVendas
import br.com.tenviti.app.telas.componentes.tela.telaVendas.CardGraficoVendas
import br.com.tenviti.app.telas.componentes.tela.telaVendas.CardsSuperioresVendas

@Composable
fun TelaVendas(
    pedidoIdParaAbrir: Long? = null,
    onPedidoIdConsumido: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val isMobile = getPlatform().name.contains("Android")

    val vm = viewModel { PedidosViewModel() }

    var mostrarModalData by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F6))
            .verticalScroll(scrollState)
            .padding(if (isMobile) 16.dp else 18.dp)
    ) {
        Text(
            text = "Vendas",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            fontSize = if (isMobile) 32.sp else 50.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        CardsSuperioresVendas(
            onFiltrarClick = { mostrarModalData = true }
        )
        Spacer(modifier = Modifier.height(24.dp))

        CardGraficoVendas(
            onFiltrarClick = { mostrarModalData = true }
        )
        Spacer(modifier = Modifier.height(24.dp))

        ListaPedidosVendas(
            pedidoInicialSelecionadoId = pedidoIdParaAbrir,
            onPedidoIdConsumido = onPedidoIdConsumido,
            viewModelExterno = vm // Passa o VM da tela
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (mostrarModalData) {
        ModalSeletorData(
            onDismiss = { mostrarModalData = false },
            onConfirm = { inicio, fim ->
                vm.carregar(inicio, fim)
                mostrarModalData = false
            }
        )
    }
}
