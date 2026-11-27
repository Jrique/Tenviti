package br.com.tenviti.server

import br.com.tenviti.server.api.Servicos
import br.com.tenviti.server.api.configurarPlugins
import br.com.tenviti.server.api.rotasApi
import br.com.tenviti.server.api.rotasVitrineWeb
import br.com.tenviti.server.api.rotasMidia
import br.com.tenviti.server.aplicacao.DadosDeExemplo
import br.com.tenviti.server.aplicacao.DonoDeExemplo
import br.com.tenviti.server.aplicacao.ServicoArquivos
import br.com.tenviti.server.aplicacao.ServicoAutenticacao
import br.com.tenviti.server.aplicacao.ServicoDepoimentos
import br.com.tenviti.server.aplicacao.ServicoCatalogo
import br.com.tenviti.server.aplicacao.ServicoLojas
import br.com.tenviti.server.aplicacao.ServicoPedidos
import br.com.tenviti.server.aplicacao.ServicoUsuarios
import br.com.tenviti.server.infra.banco.Banco
import br.com.tenviti.server.infra.banco.ConfigBanco
import br.com.tenviti.server.infra.arquivos.ArmazenamentoEmDisco
import br.com.tenviti.server.infra.banco.RepositorioCatalogoExposed
import br.com.tenviti.server.infra.banco.RepositorioDepoimentosExposed
import br.com.tenviti.server.infra.banco.RepositorioLojasExposed
import br.com.tenviti.server.infra.banco.RepositorioPedidosExposed
import br.com.tenviti.server.infra.banco.RepositorioUsuariosExposed
import br.com.tenviti.server.infra.banco.TransacoesExposed
import br.com.tenviti.server.infra.seguranca.ConfigJwt
import br.com.tenviti.server.infra.seguranca.Senhas
import br.com.tenviti.server.infra.seguranca.Tokens
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
    val diretorioVitrineWeb: File,
    val diretorioMidia: File,
    val superAdminEmail: String?,
    val superAdminSenha: String?,
    /** Lojas de exemplo criadas na subida (DADOS_EXEMPLO=true); null = não cria aquela loja. */
    val exemploThai: DonoDeExemplo? = null,
    val exemploLd: DonoDeExemplo? = null,
) {
    companion object {
        fun doAmbiente(env: Map<String, String> = System.getenv()): Configuracao {
            fun obrigatoria(nome: String) = env[nome]?.takeIf { it.isNotBlank() }
                ?: error("Variável de ambiente $nome não definida (ver .env.exemplo).")
            val comExemplos = env["DADOS_EXEMPLO"].equals("true", ignoreCase = true)
            fun dono(prefixo: String): DonoDeExemplo? {
                val email = env["${prefixo}_DONO_EMAIL"]?.takeIf { it.isNotBlank() } ?: return null
                val senha = env["${prefixo}_DONO_SENHA"]?.takeIf { it.isNotBlank() } ?: return null
                return DonoDeExemplo(email, senha).takeIf { comExemplos }
            }
            return Configuracao(
                porta = env["PORTA"]?.toIntOrNull() ?: 8081,
                banco = ConfigBanco(
                    url = env["DB_URL"] ?: "jdbc:postgresql://localhost:5432/tenviti",
                    usuario = env["DB_USUARIO"] ?: "tenviti",
                    senha = obrigatoria("DB_SENHA"),
                ),
                jwt = ConfigJwt(segredo = obrigatoria("JWT_SEGREDO")),
                origensCors = (env["CORS_ORIGENS"] ?: "http://localhost:8080").split(',').map { it.trim() }.filter { it.isNotEmpty() },
                diretorioVitrineWeb = File(env["VITRINE_WEB_DIR"] ?: "../../vitrine"),
                diretorioMidia = File(env["MIDIA_DIR"] ?: "midia"),
                superAdminEmail = env["SUPERADMIN_EMAIL"],
                superAdminSenha = env["SUPERADMIN_SENHA"],
                exemploThai = dono("THAI"),
                exemploLd = dono("LD"),
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
        pedidos = ServicoPedidos(tx, repoPedidos, repoCatalogo),
        depoimentos = ServicoDepoimentos(tx, repoDepoimentos),
        arquivos = ServicoArquivos(armazenamento),
    )

    runBlocking {
        servicos.autenticacao.garantirSuperAdmin(config.superAdminEmail, config.superAdminSenha)
        DadosDeExemplo(servicos.lojas, servicos.catalogo, servicos.depoimentos).criar(config.exemploThai, config.exemploLd)
    }

    configurarPlugins(tokens, config.origensCors)
    routing {
        rotasMidia(config.diretorioMidia)
        rotasApi(servicos)
        rotasVitrineWeb(config.diretorioVitrineWeb, servicos.lojas)
    }
}
