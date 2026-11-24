package br.com.tenviti.server.dominio

/**
 * Erros de negócio. A camada de API traduz cada tipo para um status HTTP;
 * a mensagem é escrita para ser exibida ao usuário final.
 */
sealed class ErroDeNegocio(mensagem: String, val codigo: String) : RuntimeException(mensagem)

/** Dado inválido enviado pelo usuário (HTTP 422). */
class DadoInvalido(mensagem: String, codigo: String = "dado_invalido") : ErroDeNegocio(mensagem, codigo)

/** Recurso não existe ou não pertence à loja de quem pediu (HTTP 404). */
class NaoEncontrado(mensagem: String, codigo: String = "nao_encontrado") : ErroDeNegocio(mensagem, codigo)

/** Usuário autenticado, mas sem permissão para a ação (HTTP 403). */
class AcessoNegado(mensagem: String = "Você não tem permissão para fazer isso.", codigo: String = "acesso_negado") :
    ErroDeNegocio(mensagem, codigo)

/** Conflito com o estado atual, ex.: e-mail já usado (HTTP 409). */
class Conflito(mensagem: String, codigo: String = "conflito") : ErroDeNegocio(mensagem, codigo)

/** Credenciais inválidas no login (HTTP 401). */
class CredenciaisInvalidas : ErroDeNegocio("E-mail ou senha inválidos.", "credenciais_invalidas")

internal fun exigir(condicao: Boolean, mensagem: () -> String) {
    if (!condicao) throw DadoInvalido(mensagem())
}
