package br.com.tenviti.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.tenviti.shared.StatusPedido

/** Cartão padrão: superfície com borda fina e cantos de 18 dp. */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    preenchimento: PaddingValues = PaddingValues(18.dp),
    cor: Color = TenvitiTema.cores.superficie,
    borda: Color? = TenvitiTema.cores.borda,
    espacamento: androidx.compose.ui.unit.Dp = 12.dp,
    conteudo: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = cor,
        border = borda?.let { BorderStroke(1.dp, it) },
    ) {
        Column(Modifier.padding(preenchimento), verticalArrangement = Arrangement.spacedBy(espacamento), content = conteudo)
    }
}

/** Etiqueta arredondada de status ("Novo", "Pronto"...). */
@Composable
fun Etiqueta(texto: String, cor: CorDeStatus, modifier: Modifier = Modifier) {
    Text(
        texto,
        modifier = modifier.background(cor.fundo, RoundedCornerShape(99.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        color = cor.texto,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

/** Ponto colorido (legenda, coluna do quadro). */
@Composable
fun Ponto(cor: Color, tamanho: androidx.compose.ui.unit.Dp = 10.dp) {
    Box(Modifier.size(tamanho).background(cor, CircleShape))
}

/** Cabeçalho de tela: título grande, subtítulo e ações à direita (quebram linha no celular). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CabecalhoTela(
    titulo: String,
    subtitulo: String? = null,
    sobretitulo: String? = null,
    modifier: Modifier = Modifier,
    acoes: @Composable RowScope.() -> Unit = {},
) {
    val c = TenvitiTema.cores
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // o título nunca se espreme: se não couber ao lado das ações, as ações descem
        Column(Modifier.widthIn(min = 260.dp).weight(1f, fill = false).padding(end = 12.dp)) {
            sobretitulo?.let {
                Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, fontFamily = FonteTitulo, color = c.textoSuave, letterSpacing = androidx.compose.ui.unit.TextUnit(0.6f, androidx.compose.ui.unit.TextUnitType.Sp))
            }
            Text(titulo, style = MaterialTheme.typography.headlineLarge, color = c.texto)
            subtitulo?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.textoSuave) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = acoes)
    }
}

/** Botões segmentados ("Hoje · 7 dias · 30 dias"). */
@Composable
fun SeletorSegmentado(opcoes: List<String>, selecionada: Int, onSelecionar: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = TenvitiTema.cores
    Row(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(if (c.escuro) c.superficie else Color(0xFFE7EDEC))
            .border(1.dp, if (c.escuro) c.borda else Color.Transparent, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opcoes.forEachIndexed { i, rotulo ->
            val ativa = i == selecionada
            Text(
                rotulo,
                modifier = Modifier.clip(RoundedCornerShape(9.dp))
                    .background(if (ativa) (if (c.escuro) c.borda else c.superficie) else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelecionar(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (ativa) FontWeight.SemiBold else FontWeight.Normal,
                color = if (ativa) c.texto else c.textoSuave,
            )
        }
    }
}

/** Cartão de indicador: rótulo, valor grande e detalhe. [destaque] usa o fundo petróleo escuro. */
@Composable
fun CartaoIndicador(
    rotulo: String,
    valor: String,
    detalhe: String? = null,
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    compacto: Boolean = false,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val c = TenvitiTema.cores
    Cartao(
        modifier = modifier,
        cor = if (destaque) c.destaque else c.superficie,
        borda = if (destaque) null else c.borda,
        espacamento = 8.dp,
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodySmall, color = if (destaque) c.sobreDestaqueSuave else c.textoSuave)
        Text(valor, style = if (compacto) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall, color = if (destaque) c.sobreDestaque else c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
        extra()
        detalhe?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = if (destaque) c.sobreDestaqueSuave else c.textoSuave) }
    }
}

@Composable
fun BotaoPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    Button(onClick = onClick, enabled = habilitado, modifier = modifier.height(44.dp), shape = RoundedCornerShape(12.dp)) {
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun BotaoSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    val c = TenvitiTema.cores
    OutlinedButton(
        onClick = onClick, enabled = habilitado, modifier = modifier.height(44.dp), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, c.borda),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = c.superficie, contentColor = c.texto),
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}

/** O único "destaque" coral da tela (ex.: copiar o link da vitrine). */
@Composable
fun BotaoDestaque(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = TenvitiTema.cores
    Button(
        onClick = onClick, modifier = modifier.height(40.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.acentoTexto, contentColor = if (c.escuro) Color(0xFF2A0F08) else Color.White),
    ) { Text(texto, style = MaterialTheme.typography.labelLarge) }
}

/** Cor e rótulo de cada status de pedido (status têm cores próprias, separadas da marca). */
@Composable
fun corDoStatus(status: StatusPedido): CorDeStatus {
    val c = TenvitiTema.cores
    return when (status) {
        StatusPedido.NOVO, StatusPedido.VISTO -> c.info
        StatusPedido.EM_PREPARO -> c.aviso
        StatusPedido.SAIU_PARA_ENTREGA -> CorDeStatus(c.baseSuave, c.sobreBaseSuave, c.base)
        StatusPedido.FINALIZADO -> c.sucesso
        StatusPedido.CANCELADO -> c.erro
    }
}
