package br.com.tenviti.app.telas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.tenviti.app.api.ApiProvider
import br.com.tenviti.app.data.ResumoDashboard
import br.com.tenviti.app.data.paraUi
import br.com.tenviti.app.telas.componentes.tela.telaFaturamento.PeriodoFiltroGlobal
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {
    private val api = ApiProvider.painelApi

    var dados by mutableStateOf<ResumoDashboard?>(null)
    var carregando by mutableStateOf(false)
    var erro by mutableStateOf<String?>(null)

    var filtroAtual by mutableStateOf(PeriodoFiltroGlobal.SEMANAL)

    init {
        carregar(PeriodoFiltroGlobal.SEMANAL)
    }

    fun carregar(periodo: PeriodoFiltroGlobal) {
        filtroAtual = periodo
        val dias = when (periodo) {
            PeriodoFiltroGlobal.HOJE -> 1
            PeriodoFiltroGlobal.SEMANAL -> 7
            PeriodoFiltroGlobal.MENSAL -> 30
        }
        buscar { api.resumoDashboard(dias = dias).paraUi() }
    }

    /** [inicio] e [fim] no formato AAAA-MM-DD. */
    fun carregarPersonalizado(inicio: String, fim: String) {
        buscar { api.resumoDashboard(dias = null, inicio = inicio, fim = fim).paraUi() }
    }

    private fun buscar(bloco: suspend () -> ResumoDashboard) {
        viewModelScope.launch {
            try {
                carregando = true
                erro = null
                dados = bloco()
            } catch (e: Exception) {
                erro = e.message
            } finally {
                carregando = false
            }
        }
    }
}
