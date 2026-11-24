# Projeto Kotlin (Gradle)

Módulos:

- `shared` — contratos da API (Kotlin Multiplatform: JVM, Android, JS, Wasm)
- `server` — backend Ktor (`./gradlew :server:run`, configurado por variáveis de ambiente)
- `composeApp` — painel Compose Multiplatform (`:composeApp:run`, `:composeApp:assembleDebug`,
  `:composeApp:wasmJsBrowserDevelopmentRun`)

Visão geral, variáveis de ambiente e testes: ver o [README da raiz](../README.md).
