package br.com.cardapio.app.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.cardapio.app.api.ApiProvider
import br.com.cardapio.shared.ContatoLojaDto
import br.com.cardapio.shared.DepoimentoDto
import br.com.cardapio.shared.EncomendaConfigDto
import br.com.cardapio.shared.EnderecoDto
import br.com.cardapio.shared.FuncionamentoDto
import br.com.cardapio.shared.InformacoesLojaDto
import br.com.cardapio.shared.IntervaloHorarioDto
import br.com.cardapio.shared.Modulo
import kotlinx.coroutines.launch

private val Vinho = Color(0xFF790001)
private val Fundo = Color(0xFFFFE9E6)
private val NOMES_DIAS = listOf("", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo")

/** Dados operacionais da loja: contato, endereço, horários, pausa, encomendas e depoimentos (dono e gerente). */
@Composable
fun TelaDadosLoja() {
    val api = ApiProvider.painelApi
    val escopo = rememberCoroutineScope()

    var info by remember { mutableStateOf<InformacoesLojaDto?>(null) }
    var modulos by remember { mutableStateOf<Set<Modulo>>(emptySet()) }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            info = api.informacoesLoja()
            modulos = api.minhaLoja().modulos
        } catch (e: Exception) {
            mensagem = e.message
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFFF8F6)).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Loja e horários", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        val atual = info
        if (atual == null) {
            if (mensagem == null) CircularProgressIndicator(color = Vinho) else Text(mensagem!!, color = Vinho)
            return@Column
        }
        val contato = atual.contato
        val endereco = contato.endereco ?: EnderecoDto()
        val func = atual.funcionamento
        fun atualizar(novo: InformacoesLojaDto) { info = novo; mensagem = null }

        Secao("Contato") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo("Telefone / WhatsApp (com DDD)", contato.telefone.orEmpty(), Modifier.weight(1f)) {
                    atualizar(atual.copy(contato = contato.copy(telefone = it)))
                }
                Campo("Instagram (sem @)", contato.instagram.orEmpty(), Modifier.weight(1f)) {
                    atualizar(atual.copy(contato = contato.copy(instagram = it)))
                }
            }
        }

        Secao("Endereço (aparece na retirada e no cardápio)") {
            fun mudar(e: EnderecoDto) = atualizar(atual.copy(contato = contato.copy(endereco = e)))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo("Rua", endereco.logradouro, Modifier.weight(3f)) { mudar(endereco.copy(logradouro = it)) }
                Campo("Número", endereco.numero, Modifier.weight(1f)) { mudar(endereco.copy(numero = it)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo("Complemento", endereco.complemento.orEmpty(), Modifier.weight(1f)) { mudar(endereco.copy(complemento = it)) }
                Campo("Bairro", endereco.bairro, Modifier.weight(1f)) { mudar(endereco.copy(bairro = it)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Campo("Cidade", endereco.cidade, Modifier.weight(2f)) { mudar(endereco.copy(cidade = it)) }
                Campo("UF", endereco.uf, Modifier.weight(1f)) { mudar(endereco.copy(uf = it.take(2).uppercase())) }
                Campo("CEP", endereco.cep, Modifier.weight(1f)) { mudar(endereco.copy(cep = it)) }
            }
        }

        Secao("Funcionamento") {
            fun mudar(f: FuncionamentoDto) = atualizar(atual.copy(funcionamento = f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = func.pausada, onCheckedChange = { mudar(func.copy(pausada = it)) })
                Spacer(Modifier.width(8.dp))
                Text(if (func.pausada) "Loja PAUSADA — não recebe pedidos" else "Recebendo pedidos")
            }
            if (func.pausada) {
                Campo("Mensagem para os clientes (ex.: Voltamos segunda!)", func.mensagemPausa.orEmpty(), Modifier.fillMaxWidth()) {
                    mudar(func.copy(mensagemPausa = it))
                }
            }
            Text("Horários", fontWeight = FontWeight.Bold)
            Text(
                "Sem horários cadastrados a loja fica sempre aberta. Fechamento antes da abertura = vira a noite (ex.: 18:00 às 02:00).",
                style = MaterialTheme.typography.bodySmall, color = Color.Gray,
            )
            (1..7).forEach { dia ->
                val doDia = func.horarios.filter { it.diaSemana == dia }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(NOMES_DIAS[dia], modifier = Modifier.width(80.dp))
                    if (doDia.isEmpty()) Text("Fechado", color = Color.Gray, modifier = Modifier.weight(1f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        doDia.forEach { h ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CampoHora("Abre", h.abre) { novo -> mudar(func.copy(horarios = func.horarios.map { if (it === h) h.copy(abre = novo) else it })) }
                                CampoHora("Fecha", h.fecha) { novo -> mudar(func.copy(horarios = func.horarios.map { if (it === h) h.copy(fecha = novo) else it })) }
                                IconButton(onClick = { mudar(func.copy(horarios = func.horarios.filterNot { it === h })) }) {
                                    Icon(Icons.Default.Close, "Remover horário", tint = Color.Gray)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { mudar(func.copy(horarios = func.horarios + IntervaloHorarioDto(dia, "09:00", "18:00"))) }) {
                        Text("+ horário", color = Vinho)
                    }
                }
            }
            val segunda = func.horarios.filter { it.diaSemana == 1 }
            if (segunda.isNotEmpty()) {
                TextButton(onClick = {
                    mudar(func.copy(horarios = (1..7).flatMap { d -> segunda.map { it.copy(diaSemana = d) } }))
                }) { Text("Copiar horários de segunda para todos os dias", color = Vinho) }
            }
        }

        Secao("Encomendas (pedido agendado)") {
            val enc = func.encomenda
            fun mudar(e: EncomendaConfigDto) = atualizar(atual.copy(funcionamento = func.copy(encomenda = e)))
            val moduloLigado = Modulo.AGENDAMENTO in modulos
            if (!moduloLigado) {
                Text("O módulo \"Pedido agendado\" não está ligado para sua loja. Fale com o suporte para liberar.", color = Vinho)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = enc.aceitaAgendamento, enabled = moduloLigado, onCheckedChange = {
                    mudar(enc.copy(aceitaAgendamento = it, exigeAgendamento = enc.exigeAgendamento && it))
                })
                Spacer(Modifier.width(8.dp))
                Text("Cliente pode escolher data e horário")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = enc.exigeAgendamento, enabled = moduloLigado && enc.aceitaAgendamento, onCheckedChange = {
                    mudar(enc.copy(exigeAgendamento = it))
                })
                Spacer(Modifier.width(8.dp))
                Text("Trabalho só por encomenda (todo pedido precisa de data)")
            }
            if (enc.aceitaAgendamento) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoNumero("Antecedência mínima (horas)", enc.antecedenciaMinimaHoras, Modifier.weight(1f)) { mudar(enc.copy(antecedenciaMinimaHoras = it ?: 0)) }
                    CampoNumero("Aceitar até quantos dias à frente", enc.diasMaximos, Modifier.weight(1f)) { mudar(enc.copy(diasMaximos = it ?: 14)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoNumero("Limite de pedidos por dia (vazio = sem limite)", enc.limitePedidosPorDia, Modifier.weight(1f)) { mudar(enc.copy(limitePedidosPorDia = it)) }
                    CampoNumero("Intervalo entre horários (min)", enc.intervaloMinutos, Modifier.weight(1f)) { mudar(enc.copy(intervaloMinutos = it ?: 30)) }
                }
                Text(
                    "Os horários oferecidos ao cliente seguem o funcionamento acima. O limite por dia conta pedidos (por unidade virá com os pedidos estruturados).",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                )
            }
        }

        mensagem?.let { Text(it, color = Vinho) }
        Button(
            onClick = {
                escopo.launch {
                    try {
                        salvando = true
                        info = api.salvarInformacoesLoja(atual)
                        mensagem = "Dados salvos."
                    } catch (e: Exception) {
                        mensagem = e.message
                    } finally {
                        salvando = false
                    }
                }
            },
            enabled = !salvando,
            colors = ButtonDefaults.buttonColors(containerColor = Vinho),
        ) { Text(if (salvando) "Salvando..." else "Salvar dados da loja") }

        HorizontalDivider()
        SecaoDepoimentos()
    }
}

@Composable
private fun SecaoDepoimentos() {
    val api = ApiProvider.painelApi
    val escopo = rememberCoroutineScope()
    var lista by remember { mutableStateOf(emptyList<DepoimentoDto>()) }
    var autor by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf(5) }
    var mensagem by remember { mutableStateOf<String?>(null) }

    fun recarregar() = escopo.launch {
        try { lista = api.listarDepoimentos() } catch (e: Exception) { mensagem = e.message }
    }
    LaunchedEffect(Unit) { recarregar() }

    Secao("Depoimentos de clientes (aparecem no cardápio)") {
        lista.forEach { d ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(d.autor, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            d.nota?.let { Estrelas(it, tamanho = 16.dp) }
                        }
                        Text(d.texto, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = d.ativo, onCheckedChange = { ativo ->
                        escopo.launch {
                            try { api.salvarDepoimento(d.copy(ativo = ativo)); recarregar() } catch (e: Exception) { mensagem = e.message }
                        }
                    })
                    IconButton(onClick = {
                        escopo.launch {
                            try { api.removerDepoimento(d.id); recarregar() } catch (e: Exception) { mensagem = e.message }
                        }
                    }) { Icon(Icons.Default.Delete, "Excluir", tint = Vinho) }
                }
            }
        }
        Text("Novo depoimento", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Campo("Nome do cliente", autor, Modifier.weight(1f)) { autor = it }
            Text("Nota:")
            Estrelas(nota, tamanho = 28.dp, aoEscolher = { nota = it })
        }
        Campo("O que o cliente disse", texto, Modifier.fillMaxWidth()) { texto = it }
        mensagem?.let { Text(it, color = Vinho) }
        Button(
            onClick = {
                escopo.launch {
                    try {
                        api.salvarDepoimento(DepoimentoDto(autor = autor, texto = texto, nota = nota))
                        autor = ""; texto = ""; nota = 5; mensagem = null
                        recarregar()
                    } catch (e: Exception) {
                        mensagem = e.message
                    }
                }
            },
            enabled = autor.isNotBlank() && texto.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Vinho),
        ) { Text("Adicionar depoimento") }
    }
}

/** Estrelas de 1 a 5 desenhadas com ícones (a fonte padrão do painel web não tem o caractere de estrela). */
@Composable
private fun Estrelas(nota: Int, tamanho: androidx.compose.ui.unit.Dp, aoEscolher: ((Int) -> Unit)? = null) {
    Row {
        (1..5).forEach { n ->
            Icon(
                if (n <= nota) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (aoEscolher != null) "Nota $n" else null,
                tint = Color(0xFFF5B301),
                modifier = Modifier.size(tamanho).let { m -> if (aoEscolher != null) m.clickable { aoEscolher(n) } else m },
            )
        }
    }
}

@Composable
private fun Secao(titulo: String, conteudo: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Fundo)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            conteudo()
        }
    }
}

@Composable
private fun Campo(rotulo: String, valor: String, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(value = valor, onValueChange = onChange, label = { Text(rotulo) }, singleLine = true, modifier = modifier)
}

@Composable
private fun CampoHora(rotulo: String, valor: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onChange(it.filter { c -> c.isDigit() || c == ':' }.take(5)) },
        label = { Text(rotulo) }, singleLine = true, modifier = Modifier.width(96.dp),
        isError = !Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$").matches(valor),
    )
}

@Composable
private fun CampoNumero(rotulo: String, valor: Int?, modifier: Modifier, onChange: (Int?) -> Unit) {
    var texto by remember(valor) { mutableStateOf(valor?.toString().orEmpty()) }
    OutlinedTextField(
        value = texto,
        onValueChange = { novo ->
            texto = novo.filter { it.isDigit() }.take(4)
            onChange(texto.toIntOrNull())
        },
        label = { Text(rotulo) }, singleLine = true, modifier = modifier,
    )
}
