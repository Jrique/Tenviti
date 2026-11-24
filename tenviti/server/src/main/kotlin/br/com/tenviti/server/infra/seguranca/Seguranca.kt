package br.com.tenviti.server.infra.seguranca

import at.favre.lib.crypto.bcrypt.BCrypt
import br.com.tenviti.server.dominio.Ator
import br.com.tenviti.server.dominio.Usuario
import br.com.tenviti.shared.Papel
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.time.Duration
import java.time.Instant
import java.util.Date

/** Hash de senha com BCrypt (custo 12). */
class Senhas(private val custo: Int = 12) {
    fun gerarHash(senha: String): String = BCrypt.withDefaults().hashToString(custo, senha.toCharArray())

    fun confere(senha: String, hash: String): Boolean = BCrypt.verifyer().verify(senha.toCharArray(), hash).verified

    /** Hash fixo usado quando o e-mail não existe, para o login levar o mesmo tempo e não revelar quais e-mails existem. */
    val hashFalso: String by lazy { gerarHash("senha-inexistente-para-tempo-constante") }
}

data class ConfigJwt(
    val segredo: String,
    val emissor: String = "tenviti",
    val audiencia: String = "tenviti-painel",
    val validade: Duration = Duration.ofHours(12),
) {
    init {
        require(segredo.length >= 32) { "JWT_SEGREDO precisa ter pelo menos 32 caracteres (gere com: openssl rand -hex 32)." }
    }
}

class Tokens(private val config: ConfigJwt) {
    private val algoritmo = Algorithm.HMAC256(config.segredo)

    val verificador: JWTVerifier = JWT.require(algoritmo)
        .withIssuer(config.emissor)
        .withAudience(config.audiencia)
        .build()

    val emissor get() = config.emissor
    val audiencia get() = config.audiencia

    fun gerar(usuario: Usuario, agora: Instant = Instant.now()): String =
        JWT.create()
            .withIssuer(config.emissor)
            .withAudience(config.audiencia)
            .withSubject(usuario.id.toString())
            .withClaim(CLAIM_PAPEL, usuario.papel.name)
            .withClaim(CLAIM_LOJA, usuario.lojaId)
            .withIssuedAt(Date.from(agora))
            .withExpiresAt(Date.from(agora.plus(config.validade)))
            .sign(algoritmo)

    companion object {
        const val CLAIM_PAPEL = "papel"
        const val CLAIM_LOJA = "loja"

        fun atorDe(subject: String?, papel: String?, loja: Long?): Ator? {
            val id = subject?.toLongOrNull() ?: return null
            val p = Papel.entries.find { it.name == papel } ?: return null
            return Ator(id, p, loja)
        }
    }
}
