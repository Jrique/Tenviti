package br.com.tenviti.app

import br.com.tenviti.app.data.UsuarioSessao
import br.com.tenviti.shared.Papel

enum class Tela(val titulo: String) {
    DASHBOARD("Dashboard"),
    PRODUTOS("Produtos"),
    CATEGORIAS("Categorias"),
    FATURAMENTO("Faturamento"),
    VENDAS("Vendas"),
    DADOS_LOJA("Loja e horários"),
    APARENCIA("Aparência da loja"),
    CONFIGURACOES("Configurações");

    /** Quais telas cada papel enxerga no menu (o servidor também bloqueia o que não é permitido). */
    fun visivelPara(usuario: UsuarioSessao): Boolean = when (this) {
        DASHBOARD, FATURAMENTO -> usuario.vePainelFinanceiro
        PRODUTOS, CATEGORIAS -> usuario.editaCatalogo
        VENDAS -> usuario.papel in PAPEIS_QUE_ATENDEM
        DADOS_LOJA -> usuario.gerenciaEquipe
        APARENCIA -> usuario.personalizaLoja
        CONFIGURACOES -> true
    }

    companion object {
        val PAPEIS_QUE_ATENDEM = setOf(Papel.DONO, Papel.GERENTE, Papel.ATENDENTE, Papel.COZINHA)

        fun inicialPara(usuario: UsuarioSessao): Tela = entries.first { it.visivelPara(usuario) }
    }
}
