package br.com.tenviti.server.dominio

class Categoria(
    val id: Long,
    val lojaId: Long,
    nome: String,
    ordem: Int,
) {
    var nome: String = nome; private set
    var ordem: Int = ordem; private set

    init {
        validarNome(nome)
    }

    fun renomear(nome: String, ordem: Int) {
        validarNome(nome)
        this.nome = nome.trim()
        this.ordem = ordem
    }

    companion object {
        fun nova(lojaId: Long, nome: String, ordem: Int) = Categoria(0, lojaId, nome.trim(), ordem)

        private fun validarNome(nome: String) = exigir(nome.isNotBlank() && nome.length <= 80) {
            "O nome da categoria é obrigatório e pode ter no máximo 80 caracteres."
        }
    }
}

/** Uma escolha dentro de um grupo (ex.: "Nutella", "Bacon +R$ 4,00"). */
data class Opcao(
    val id: Long = 0,
    val nome: String,
    val precoCentavos: Long = 0,
    val ativo: Boolean = true,
) {
    init {
        exigir(nome.isNotBlank() && nome.length <= 80) { "O nome da opção é obrigatório e pode ter no máximo 80 caracteres." }
        exigir(precoCentavos >= 0) { "O preço da opção não pode ser negativo." }
    }
}

/**
 * Grupo de opções de um produto, com quantidade mínima e máxima de escolhas.
 * Ex.: "Escolha 4 sabores" (4 a 4, pode repetir) ou "Adicionais" (0 a 3).
 */
data class GrupoOpcoes(
    val id: Long = 0,
    val nome: String,
    val minimo: Int = 0,
    val maximo: Int = 1,
    val permiteRepetir: Boolean = false,
    val opcoes: List<Opcao>,
) {
    init {
        exigir(nome.isNotBlank() && nome.length <= 80) { "O nome do grupo de opções é obrigatório (máximo 80 caracteres)." }
        exigir(minimo >= 0) { "No grupo \"$nome\", o mínimo não pode ser negativo." }
        exigir(maximo in 1..50) { "No grupo \"$nome\", o máximo deve ficar entre 1 e 50." }
        exigir(minimo <= maximo) { "No grupo \"$nome\", o mínimo não pode ser maior que o máximo." }
        exigir(opcoes.isNotEmpty() && opcoes.size <= 100) { "O grupo \"$nome\" precisa ter de 1 a 100 opções." }
        val ativas = opcoes.count { it.ativo }
        exigir(permiteRepetir || minimo <= ativas) {
            "No grupo \"$nome\", o mínimo ($minimo) é maior que o número de opções disponíveis ($ativas). " +
                "Diminua o mínimo ou permita repetir opções."
        }
    }

    val obrigatorio: Boolean get() = minimo > 0
}

class Produto(
    val id: Long,
    val lojaId: Long,
    categoriaId: Long?,
    nome: String,
    descricao: String,
    precoCentavos: Long,
    precoPromocionalCentavos: Long?,
    imagemUrl: String?,
    grupos: List<GrupoOpcoes>,
    destaque: Boolean,
    ativo: Boolean,
) {
    var categoriaId: Long? = categoriaId; private set
    var nome: String = nome; private set
    var descricao: String = descricao; private set
    var precoCentavos: Long = precoCentavos; private set
    var precoPromocionalCentavos: Long? = precoPromocionalCentavos; private set
    var imagemUrl: String? = imagemUrl; private set
    var grupos: List<GrupoOpcoes> = grupos; private set
    var destaque: Boolean = destaque; private set
    var ativo: Boolean = ativo; private set

    init {
        validar(nome, descricao, precoCentavos, precoPromocionalCentavos, imagemUrl, grupos)
    }

    /** Preço cobrado hoje (o promocional, quando houver). */
    val precoAtualCentavos: Long get() = precoPromocionalCentavos ?: precoCentavos

    fun atualizar(
        categoriaId: Long?,
        nome: String,
        descricao: String,
        precoCentavos: Long,
        precoPromocionalCentavos: Long?,
        imagemUrl: String?,
        grupos: List<GrupoOpcoes>,
        destaque: Boolean,
        ativo: Boolean,
    ) {
        validar(nome, descricao, precoCentavos, precoPromocionalCentavos, imagemUrl, grupos)
        this.categoriaId = categoriaId
        this.nome = nome.trim()
        this.descricao = descricao.trim()
        this.precoCentavos = precoCentavos
        this.precoPromocionalCentavos = precoPromocionalCentavos
        this.imagemUrl = imagemUrl?.trim()?.ifEmpty { null }
        this.grupos = grupos
        this.destaque = destaque
        this.ativo = ativo
    }

    fun marcarDestaque(destaque: Boolean) {
        this.destaque = destaque
    }

    /** Pausa (esgotado) ou volta a vender, sem apagar o produto. */
    fun definirDisponivel(disponivel: Boolean) {
        this.ativo = disponivel
    }

    /**
     * Monta o item do pedido a partir do que o cliente escolheu na vitrine, conferindo as regras do
     * produto: disponível, opções que existem e estão ativas, mínimo/máximo de cada grupo e repetição.
     * O preço vem daqui (do catálogo), nunca do navegador.
     */
    fun montarItem(quantidade: Int, escolhas: List<EscolhaDeOpcao>, observacao: String?): ItemPedido {
        if (!ativo) throw Conflito("\"$nome\" não está disponível no momento. Tire da sacola para continuar.", "produto_indisponivel")
        exigir(quantidade in 1..99) { "Quantidade de \"$nome\" inválida (de 1 a 99)." }
        val obs = observacao?.trim()?.ifEmpty { null }
        exigir((obs?.length ?: 0) <= 200) { "A observação de \"$nome\" pode ter no máximo 200 caracteres." }
        exigir(escolhas.all { it.quantidade in 1..50 }) { "Quantidade de opção inválida em \"$nome\"." }

        // soma escolhas repetidas da mesma opção
        val porOpcao = escolhas.groupBy { it.opcaoId }.mapValues { (_, lista) -> lista.sumOf { it.quantidade } }
        val conhecidas = grupos.flatMap { g -> g.opcoes.filter { it.ativo }.map { it.id } }.toSet()
        porOpcao.keys.firstOrNull { it !in conhecidas }?.let {
            throw DadoInvalido("Uma das opções escolhidas em \"$nome\" não está mais disponível. Escolha de novo.", "opcao_invalida")
        }

        val opcoes = grupos.flatMap { grupo ->
            val escolhidas = grupo.opcoes.filter { it.ativo && it.id in porOpcao }
            val total = escolhidas.sumOf { porOpcao.getValue(it.id) }
            exigir(total >= grupo.minimo) {
                if (grupo.minimo == grupo.maximo) "Em \"$nome\", escolha ${grupo.minimo} em \"${grupo.nome}\"."
                else "Em \"$nome\", escolha pelo menos ${grupo.minimo} em \"${grupo.nome}\"."
            }
            exigir(total <= grupo.maximo) { "Em \"$nome\", escolha no máximo ${grupo.maximo} em \"${grupo.nome}\"." }
            exigir(grupo.permiteRepetir || escolhidas.all { porOpcao.getValue(it.id) == 1 }) {
                "Em \"$nome\", cada opção de \"${grupo.nome}\" só pode ser escolhida uma vez."
            }
            escolhidas.map { OpcaoDoItem(grupo.nome, it.nome, it.precoCentavos, porOpcao.getValue(it.id)) }
        }
        return ItemPedido(id, nome, quantidade, precoAtualCentavos, opcoes, obs)
    }

    companion object {
        fun novo(
            lojaId: Long,
            categoriaId: Long?,
            nome: String,
            descricao: String,
            precoCentavos: Long,
            precoPromocionalCentavos: Long?,
            imagemUrl: String?,
            grupos: List<GrupoOpcoes>,
            destaque: Boolean,
            ativo: Boolean,
        ) = Produto(
            0, lojaId, categoriaId, nome.trim(), descricao.trim(), precoCentavos, precoPromocionalCentavos,
            imagemUrl?.trim()?.ifEmpty { null }, grupos, destaque, ativo,
        )

        private fun validar(
            nome: String,
            descricao: String,
            precoCentavos: Long,
            precoPromocionalCentavos: Long?,
            imagemUrl: String?,
            grupos: List<GrupoOpcoes>,
        ) {
            exigir(nome.isNotBlank() && nome.length <= 120) { "O nome do produto é obrigatório e pode ter no máximo 120 caracteres." }
            exigir(descricao.length <= 1000) { "A descrição pode ter no máximo 1000 caracteres." }
            exigir(precoCentavos > 0) { "O preço do produto deve ser maior que zero." }
            exigir(precoPromocionalCentavos == null || precoPromocionalCentavos in 1 until precoCentavos) {
                "O preço promocional deve ser maior que zero e menor que o preço normal."
            }
            exigir(imagemUrl.isNullOrBlank() || urlDeImagemValida(imagemUrl)) { "Endereço da foto inválido." }
            exigir(grupos.size <= 20) { "Um produto pode ter no máximo 20 grupos de opções." }
        }

        fun urlDeImagemValida(url: String): Boolean =
            url.length <= 500 && (url.startsWith("https://") || url.startsWith("http://") || url.startsWith("/"))
    }
}

/** Depoimento de cliente exibido no cardápio. */
class Depoimento(
    val id: Long,
    val lojaId: Long,
    autor: String,
    texto: String,
    nota: Int?,
    ativo: Boolean,
    ordem: Int,
) {
    var autor: String = autor; private set
    var texto: String = texto; private set
    var nota: Int? = nota; private set
    var ativo: Boolean = ativo; private set
    var ordem: Int = ordem; private set

    init {
        validar(autor, texto, nota)
    }

    fun atualizar(autor: String, texto: String, nota: Int?, ativo: Boolean, ordem: Int) {
        validar(autor, texto, nota)
        this.autor = autor.trim()
        this.texto = texto.trim()
        this.nota = nota
        this.ativo = ativo
        this.ordem = ordem
    }

    companion object {
        fun novo(lojaId: Long, autor: String, texto: String, nota: Int?, ativo: Boolean, ordem: Int) =
            Depoimento(0, lojaId, autor.trim(), texto.trim(), nota, ativo, ordem)

        private fun validar(autor: String, texto: String, nota: Int?) {
            exigir(autor.isNotBlank() && autor.length <= 80) { "Informe o nome de quem deu o depoimento (máximo 80 caracteres)." }
            exigir(texto.isNotBlank() && texto.length <= 500) { "O depoimento é obrigatório e pode ter no máximo 500 caracteres." }
            exigir(nota == null || nota in 1..5) { "A nota deve ser de 1 a 5." }
        }
    }
}
