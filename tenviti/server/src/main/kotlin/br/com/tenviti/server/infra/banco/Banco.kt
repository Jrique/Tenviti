package br.com.tenviti.server.infra.banco

import br.com.tenviti.server.dominio.Conflito
import br.com.tenviti.server.dominio.Transacoes
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

data class ConfigBanco(val url: String, val usuario: String, val senha: String, val maxConexoes: Int = 10)

/** Abre o pool de conexões, aplica as migrações (Flyway) e conecta o Exposed. */
class Banco(config: ConfigBanco) : AutoCloseable {
    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.url
            username = config.usuario
            password = config.senha
            maximumPoolSize = config.maxConexoes
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
        },
    )

    val database: Database = Database.connect(dataSource)

    fun migrar() {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migracao")
            .load()
            .migrate()
    }

    override fun close() = dataSource.close()
}

class TransacoesExposed(private val banco: Banco) : Transacoes {
    override suspend fun <T> executar(bloco: () -> T): T =
        try {
            newSuspendedTransaction(Dispatchers.IO, banco.database) { bloco() }
        } catch (e: ExposedSQLException) {
            // 23505 = violação de chave única (ex.: dois cadastros simultâneos com o mesmo e-mail)
            if (e.sqlState == "23505") throw Conflito("Já existe um cadastro com esses dados.")
            throw e
        }
}
