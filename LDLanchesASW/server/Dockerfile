# Imagem do servidor. Build a partir da raiz do repositório:
#   docker build -f LDLanchesASW/server/Dockerfile -t cardapio-servidor .
# (o docker-compose.yml já faz isso)

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY LDLanchesASW/ ./
# -PsomenteServidor: configura só os módulos shared (JVM) e server, sem precisar do Android SDK
RUN ./gradlew --no-daemon --console=plain -PsomenteServidor=true :server:installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 cardapio && mkdir -p /app/midia && chown cardapio /app/midia
COPY --from=build /src/server/build/install/server/ /app/
COPY CardapioWeb/ /app/cardapio-web/
USER cardapio
ENV PORTA=8081 CARDAPIO_WEB_DIR=/app/cardapio-web MIDIA_DIR=/app/midia
EXPOSE 8081
ENTRYPOINT ["/app/bin/server"]
