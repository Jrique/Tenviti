package br.com.tenviti.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.tenviti.app.data.UsuarioSessao
import br.com.tenviti.shared.Papel

enum class Tela(val titulo: String, val icone: ImageVector) {
    HOJE("Hoje", Icons.Outlined.Home),
    PEDIDOS("Pedidos", Icons.AutoMirrored.Outlined.ReceiptLong),
    CATALOGO("Catálogo", Icons.Outlined.Inventory2),
    FINANCEIRO("Financeiro", Icons.Outlined.AccountBalanceWallet),
    LOJA("Loja e horários", Icons.Outlined.Storefront),
    APARENCIA("Aparência da vitrine", Icons.Outlined.Palette),
    CONFIGURACOES("Configurações", Icons.Outlined.Settings);

    /** Quais telas cada papel enxerga no menu (o servidor também bloqueia o que não é permitido). */
    fun visivelPara(usuario: UsuarioSessao): Boolean = when (this) {
        HOJE -> true
        PEDIDOS -> usuario.papel in PAPEIS_QUE_ATENDEM
        CATALOGO -> usuario.editaCatalogo
        FINANCEIRO -> usuario.vePainelFinanceiro
        LOJA -> usuario.gerenciaEquipe
        APARENCIA -> usuario.personalizaLoja
        CONFIGURACOES -> true
    }

    companion object {
        val PAPEIS_QUE_ATENDEM = setOf(Papel.DONO, Papel.GERENTE, Papel.ATENDENTE, Papel.COZINHA)

        /** As que ficam no menu inferior do celular; as outras vão para "Mais". */
        val PRINCIPAIS_NO_CELULAR = listOf(HOJE, PEDIDOS, CATALOGO)

        fun inicialPara(usuario: UsuarioSessao): Tela = entries.first { it.visivelPara(usuario) }
    }
}
