package br.com.cardapio.server

import br.com.cardapio.server.api.Servicos
import br.com.cardapio.server.api.configurarPlugins
import br.com.cardapio.server.api.rotasApi
import br.com.cardapio.server.api.rotasCardapioWeb
import br.com.cardapio.server.api.rotasMidia
import br.com.cardapio.server.aplicacao.ServicoArquivos
import br.com.cardapio.server.aplicacao.ServicoAutenticacao
import br.com.cardapio.server.aplicacao.ServicoDepoimentos
import br.com.cardapio.server.aplicacao.ServicoCatalogo
import br.com.cardapio.server.aplicacao.ServicoLojas
import br.com.cardapio.server.aplicacao.ServicoPedidos
import br.com.cardapio.server.aplicacao.ServicoUsuarios
import br.com.cardapio.server.infra.banco.Banco
import br.com.cardapio.server.infra.banco.ConfigBanco
import br.com.cardapio.server.infra.arquivos.ArmazenamentoEmDisco
import br.com.cardapio.server.infra.banco.RepositorioCatalogoExposed
import br.com.cardapio.server.infra.banco.RepositorioDepoimentosExposed
import br.com.cardapio.server.infra.banco.RepositorioLojasExposed
import br.com.cardapio.server.infra.banco.RepositorioPedidosExposed
import br.com.cardapio.server.infra.banco.RepositorioUsuariosExposed
import br.com.cardapio.server.infra.banco.TransacoesExposed
import br.com.cardapio.server.infra.seguranca.ConfigJwt
import br.com.cardapio.server.infra.seguranca.Senhas
import br.com.cardapio.server.infra.seguranca.Tokens
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import java.io.File

/** Configuração lida das variáveis de ambiente (ver .env.exemplo na raiz do repositório). */
data class Configuracao(
    val porta: Int,
    val banco: ConfigBanco,
    val jwt: ConfigJwt,
    val origensCors: List<String>,
    val diretorioCardapioWeb: File,
    val diretorioMidia: File,
    val superAdminEmail: String?,
    val superAdminSenha: String?,
) {
    companion object {
        fun doAmbiente(env: Map<String, String> = System.getenv()): Configuracao {
            fun obrigatoria(nome: String) = env[nome]?.takeIf { it.isNotBlank() }
                ?: error("Variável de ambiente $nome não definida (ver .env.exemplo).")
            return Configuracao(
                porta = env["PORTA"]?.toIntOrNull() ?: 8081,
                banco = ConfigBanco(
                    url = env["DB_URL"] ?: "jdbc:postgresql://localhost:5432/cardapio",
                    usuario = env["DB_USUARIO"] ?: "cardapio",
                    senha = obrigatoria("DB_SENHA"),
                ),
                jwt = ConfigJwt(segredo = obrigatoria("JWT_SEGREDO")),
                origensCors = (env["CORS_ORIGENS"] ?: "http://localhost:8080").split(',').map { it.trim() }.filter { it.isNotEmpty() },
                diretorioCardapioWeb = File(env["CARDAPIO_WEB_DIR"] ?: "../../CardapioWeb"),
                diretorioMidia = File(env["MIDIA_DIR"] ?: "midia"),
                superAdminEmail = env["SUPERADMIN_EMAIL"],
                superAdminSenha = env["SUPERADMIN_SENHA"],
            )
        }
    }
}

fun main() {
    val config = Configuracao.doAmbiente()
    embeddedServer(Netty, port = config.porta, host = "0.0.0.0") { modulo(config) }.start(wait = true)
}

/** Monta a aplicação: banco + migrações, dependências, plugins e rotas. */
fun Application.modulo(config: Configuracao) {
    val banco = Banco(config.banco).also { it.migrar() }
    monitor.subscribe(ApplicationStopped) { banco.close() }

    // Infra
    val tx = TransacoesExposed(banco)
    val repoLojas = RepositorioLojasExposed()
    val repoUsuarios = RepositorioUsuariosExposed()
    val repoCatalogo = RepositorioCatalogoExposed()
    val repoPedidos = RepositorioPedidosExposed()
    val repoDepoimentos = RepositorioDepoimentosExposed()
    val armazenamento = ArmazenamentoEmDisco(config.diretorioMidia)
    val senhas = Senhas()
    val tokens = Tokens(config.jwt)

    // Aplicação
    val servicos = Servicos(
        autenticacao = ServicoAutenticacao(tx, repoUsuarios, repoLojas, senhas, tokens),
        lojas = ServicoLojas(tx, repoLojas, repoUsuarios, senhas),
        usuarios = ServicoUsuarios(tx, repoUsuarios, senhas),
        catalogo = ServicoCatalogo(tx, repoCatalogo),
        pedidos = ServicoPedidos(tx, repoPedidos),
        depoimentos = ServicoDepoimentos(tx, repoDepoimentos),
        arquivos = ServicoArquivos(armazenamento),
    )

    runBlocking { servicos.autenticacao.garantirSuperAdmin(config.superAdminEmail, config.superAdminSenha) }

    configurarPlugins(tokens, config.origensCors)
    routing {
        rotasMidia(config.diretorioMidia)
        rotasApi(servicos)
        rotasCardapioWeb(config.diretorioCardapioWeb, servicos.lojas)
    }
}
