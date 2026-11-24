package br.com.tenviti.server

import org.flywaydb.core.Flyway
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A V2 converte o antigo campo texto de adicionais ("Bacon:4,00; Ovo:2") em grupo "Adicionais" com opções.
 * Aplica só a V1, grava dados no formato antigo, aplica a V2 e confere o resultado.
 */
class MigracaoV2Test : TesteDeApi() {

    private fun flyway(alvo: String?) = Flyway.configure()
        .dataSource(banco.url, banco.usuario, banco.senha)
        .locations("classpath:db/migracao")
        .apply { if (alvo != null) target(alvo) }
        .load()

    @Test
    fun adicionaisEmTextoViramGrupoDeOpcoes() {
        flyway("1").migrate()
        DriverManager.getConnection(banco.url, banco.usuario, banco.senha).use { c ->
            c.createStatement().use { s ->
                s.execute("insert into loja (id, slug, nome) values (1, 'ld', 'LD')")
                s.execute(
                    """
                    insert into produto (id, loja_id, nome, preco_centavos, adicionais) values
                      (10, 1, 'X-Tudo', 3290, 'Bacon:4,00; Ovo:2; Cheddar: 1.234,50 ; Molho:abc'),
                      (11, 1, 'Refri', 600, ''),
                      (12, 1, 'X-Salada', 2490, 'Picles:0,5')
                    """.trimIndent(),
                )
                s.execute("insert into pedido (loja_id, codigo, descricao, total_centavos, status) values (1, 'ABC', 'x', 100, 'NOVO')")
            }
        }

        flyway(null).migrate()

        DriverManager.getConnection(banco.url, banco.usuario, banco.senha).use { c ->
            val opcoes = c.createStatement().use { s ->
                s.executeQuery(
                    """
                    select p.nome as produto, g.nome as grupo, o.nome as opcao, o.preco_centavos
                    from opcao o join grupo_opcao g on g.id = o.grupo_id join produto p on p.id = g.produto_id
                    order by p.id, o.ordem
                    """.trimIndent(),
                ).use { rs ->
                    buildList { while (rs.next()) add("${rs.getString(1)}|${rs.getString(2)}|${rs.getString(3)}|${rs.getLong(4)}") }
                }
            }
            assertEquals(
                listOf(
                    "X-Tudo|Adicionais|Bacon|400",
                    "X-Tudo|Adicionais|Ovo|200",
                    "X-Tudo|Adicionais|Cheddar|123450",
                    "X-Tudo|Adicionais|Molho|0", // preço ilegível vira zero (dono corrige no painel)
                    "X-Salada|Adicionais|Picles|50",
                ),
                opcoes,
            )
            // pedido antigo ganhou dia de produção
            val diaPreenchido = c.createStatement().use { s ->
                s.executeQuery("select count(*) from pedido where dia_producao is not null").use { it.next(); it.getInt(1) }
            }
            assertEquals(1, diaPreenchido)
        }
    }
}
