// Backend da plataforma (Ktor Server + PostgreSQL).
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "br.com.tenviti"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("br.com.tenviti.server.AplicacaoKt")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.banco)
    implementation(libs.bcrypt)
    implementation(libs.logback)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content)
    testImplementation(libs.kotlin.testJunit5)
}

tasks.test {
    useJUnitPlatform()
    // Os testes de integração usam um PostgreSQL real (ver README: "Rodando os testes").
    environment("DB_URL_TESTE", System.getenv("DB_URL_TESTE") ?: "jdbc:postgresql://localhost:5432/tenviti_teste")
    environment("DB_USUARIO_TESTE", System.getenv("DB_USUARIO_TESTE") ?: "tenviti")
    environment("DB_SENHA_TESTE", System.getenv("DB_SENHA_TESTE") ?: "tenviti")
}
