package br.com.tenviti.app.telas

import br.com.tenviti.app.ui.TenvitiTema

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.tenviti.app.getPlatform
import br.com.tenviti.app.telas.componentes.tela.telaFaturamento.*
import br.com.tenviti.app.telas.componentes.tela.telaFaturamento.ModalSeletorData

@Composable
fun TelaFaturamento() {
    val vm = viewModel { DashboardViewModel() }
    val dados = vm.dados

    var mostrarModalData by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val isMobile = getPlatform().name.contains("Android")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TenvitiTema.cores.fundo)
            .padding(horizontal = if (isMobile) 16.dp else 32.dp)
            .verticalScroll(scrollState)
    ) {
        // Cabeçalho com filtros pré-definidos (Hoje, Semanal, Mensal)
        CabecalhoFaturamento(
            periodoSelecionado = vm.filtroAtual,
            onPeriodoSelecionado = { novoPeriodo ->
                vm.carregar(novoPeriodo)
            }
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (vm.carregando) {
            androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        CardsResumoFaturamento(
            dados = dados,
            onFiltrarClick = { mostrarModalData = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        CardGraficoFaturamento(
            dados = dados,
            onFiltrarClick = { mostrarModalData = true }
        )
    }

    if (mostrarModalData) {
        ModalSeletorData(
            onDismiss = { mostrarModalData = false },
            onConfirm = { inicio, fim ->
                vm.carregarPersonalizado(inicio, fim)
                mostrarModalData = false
            }
        )
    }
}