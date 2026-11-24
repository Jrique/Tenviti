package br.com.cardapio.shared

import kotlin.math.abs

/** Regras de formato reutilizadas pelo servidor (validação) e pelo app (validação antecipada no formulário). */
object Formatos {
    private val SLUG = Regex("^[a-z0-9](?:[a-z0-9-]{1,48}[a-z0-9])$")
    private val COR_HEX = Regex("^#[0-9A-Fa-f]{6}$")
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** Caminhos que não podem virar slug de loja porque são usados pelo próprio sistema. */
    val SLUGS_RESERVADOS = setOf("api", "admin", "painel", "saude", "static", "assets", "login", "midia")

    const val SENHA_TAMANHO_MINIMO = 8

    fun slugValido(slug: String): Boolean = SLUG.matches(slug) && slug !in SLUGS_RESERVADOS
    fun corValida(cor: String): Boolean = COR_HEX.matches(cor)
    fun emailValido(email: String): Boolean = EMAIL.matches(email.trim())
    fun senhaValida(senha: String): Boolean = senha.length >= SENHA_TAMANHO_MINIMO

    /** Gera um slug sugerido a partir do nome da loja ("LD Lanches" -> "ld-lanches"). */
    fun sugerirSlug(nome: String): String {
        val semAcento = buildString {
            for (c in nome.lowercase()) append(ACENTOS[c] ?: c)
        }
        return semAcento
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(50)
    }

    private val ACENTOS = mapOf(
        'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
        'é' to 'e', 'ê' to 'e', 'è' to 'e', 'ë' to 'e',
        'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
        'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
        'ç' to 'c', 'ñ' to 'n',
    )
}

/** Formata centavos como moeda brasileira: 123456 -> "R$ 1.234,56". */
fun Long.emReais(): String {
    val negativo = this < 0
    val valor = abs(this)
    val reais = (valor / 100).toString()
    val centavos = (valor % 100).toString().padStart(2, '0')
    val reaisComMilhar = reais.reversed().chunked(3).joinToString(".").reversed()
    return (if (negativo) "-R$ " else "R$ ") + "$reaisComMilhar,$centavos"
}

/**
 * Converte texto digitado ("25,90", "25.90", "R$ 1.234,50") em centavos.
 * Retorna null se não for um valor válido.
 */
fun String.reaisParaCentavos(): Long? {
    val limpo = this.replace("R$", "").replace(" ", "").trim()
    if (limpo.isEmpty()) return null
    val normalizado = if (limpo.contains(',')) limpo.replace(".", "").replace(',', '.') else limpo
    val partes = normalizado.split('.')
    if (partes.size > 2 || partes.any { p -> p.any { !it.isDigit() } }) return null
    val reais = partes[0].ifEmpty { "0" }.toLongOrNull() ?: return null
    val centavosTexto = partes.getOrNull(1) ?: ""
    if (centavosTexto.length > 2) return null
    val centavos = centavosTexto.padEnd(2, '0').toLongOrNull() ?: 0
    return reais * 100 + centavos
}
