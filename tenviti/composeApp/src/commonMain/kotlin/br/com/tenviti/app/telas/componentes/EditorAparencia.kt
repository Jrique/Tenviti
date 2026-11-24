package br.com.tenviti.app.telas.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.tenviti.shared.AparenciaDto
import br.com.tenviti.shared.Formatos
import br.com.tenviti.shared.ItemPersonalizavel
import br.com.tenviti.shared.ModeloLayout

/**
 * Formulário de aparência da loja, usado pelo super admin (tudo liberado) e pelo dono
 * (só os itens que o super admin liberou ficam editáveis).
 */
@Composable
fun EditorAparencia(
    aparencia: AparenciaDto,
    liberado: (ItemPersonalizavel) -> Boolean,
    onChange: (AparenciaDto) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Secao("Cores", liberado(ItemPersonalizavel.CORES)) { habilitado ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CampoCor("Principal", aparencia.corPrimaria, habilitado, Modifier.weight(1f)) { onChange(aparencia.copy(corPrimaria = it)) }
                CampoCor("Destaque", aparencia.corSecundaria, habilitado, Modifier.weight(1f)) { onChange(aparencia.copy(corSecundaria = it)) }
                CampoCor("Fundo", aparencia.corFundo, habilitado, Modifier.weight(1f)) { onChange(aparencia.copy(corFundo = it)) }
            }
        }
        Secao("Logo", liberado(ItemPersonalizavel.LOGO)) { habilitado ->
            OutlinedTextField(
                value = aparencia.logoUrl.orEmpty(), onValueChange = { onChange(aparencia.copy(logoUrl = it)) },
                label = { Text("Endereço (URL) da imagem do logo") }, enabled = habilitado, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Secao("Banners e textos", liberado(ItemPersonalizavel.BANNERS_E_TEXTOS)) { habilitado ->
            OutlinedTextField(
                value = aparencia.bannerUrl.orEmpty(), onValueChange = { onChange(aparencia.copy(bannerUrl = it)) },
                label = { Text("Endereço (URL) do banner") }, enabled = habilitado, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = aparencia.mensagemBoasVindas.orEmpty(), onValueChange = { onChange(aparencia.copy(mensagemBoasVindas = it)) },
                label = { Text("Mensagem de boas-vindas") }, enabled = habilitado, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = aparencia.aviso.orEmpty(), onValueChange = { onChange(aparencia.copy(aviso = it)) },
                label = { Text("Aviso no topo do cardápio (ex.: fechado na segunda)") }, enabled = habilitado,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Secao("Modelo de layout", liberado(ItemPersonalizavel.LAYOUT)) { habilitado ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModeloLayout.entries.forEach { modelo ->
                    FilterChip(
                        selected = aparencia.modeloLayout == modelo,
                        onClick = { onChange(aparencia.copy(modeloLayout = modelo)) },
                        label = { Text(modelo.rotulo) },
                        enabled = habilitado,
                    )
                }
            }
        }
        Secao("CSS extra (avançado)", liberado(ItemPersonalizavel.CSS_EXTRA)) { habilitado ->
            OutlinedTextField(
                value = aparencia.cssExtra.orEmpty(), onValueChange = { onChange(aparencia.copy(cssExtra = it)) },
                label = { Text("Regras CSS aplicadas ao cardápio web") }, enabled = habilitado, minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Secao(titulo: String, habilitado: Boolean, conteudo: @Composable ColumnScope.(Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, fontWeight = FontWeight.Bold)
            if (!habilitado) {
                Spacer(Modifier.width(8.dp))
                Text("(bloqueado — fale com o suporte)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        conteudo(habilitado)
    }
}

@Composable
private fun CampoCor(rotulo: String, valor: String, habilitado: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            Modifier.size(28.dp)
                .background(corDoHex(valor) ?: Color.Transparent, RoundedCornerShape(6.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = valor, onValueChange = onChange, label = { Text(rotulo) }, enabled = habilitado, singleLine = true,
            isError = !Formatos.corValida(valor),
        )
    }
}

fun corDoHex(hex: String): Color? =
    if (Formatos.corValida(hex)) Color(0xFF000000 or hex.removePrefix("#").toLong(16)) else null
