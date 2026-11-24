# Imagem do servidor Tenviti. Build a partir da raiz do repositório:
#   docker build -f tenviti/server/Dockerfile -t tenviti-servidor .
# (o docker-compose.yml já faz isso)

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY tenviti/ ./
# -PsomenteServidor: configura só os módulos shared (JVM) e server, sem precisar do Android SDK
RUN ./gradlew --no-daemon --console=plain -PsomenteServidor=true :server:installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 tenviti && mkdir -p /app/midia && chown tenviti /app/midia
COPY --from=build /src/server/build/install/server/ /app/
COPY vitrine/ /app/vitrine/
USER tenviti
ENV PORTA=8081 VITRINE_WEB_DIR=/app/vitrine MIDIA_DIR=/app/midia
EXPOSE 8081
ENTRYPOINT ["/app/bin/server"]
