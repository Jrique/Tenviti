# Tenviti

> **Tem. Vi. Tive.** A loja tem, o cliente vê, o pedido acontece.

Sistema de pedidos e gestão para lojas de alimentação, oferecido como **SaaS para várias lojas**
(multi-loja). Cada loja tem seu cardápio online, sua equipe, seus pedidos e sua própria aparência.

## Estrutura do repositório

```
CardapioWeb/          Cardápio público (HTML/CSS/JS leve) — servido pelo servidor em /{loja}/
LDLanchesASW/
  shared/             Contratos da API (DTOs, enums, formatos) — Kotlin Multiplatform
  server/             Backend Ktor + PostgreSQL (Exposed + Flyway)
  composeApp/         Painel de gestão (Android, Desktop e Web) — Compose Multiplatform
infra/postgres/       Scripts de inicialização do PostgreSQL local
docker-compose.yml    PostgreSQL + servidor
.env.exemplo          Variáveis de ambiente (copie para .env)
```

## Como funciona o multi-loja

| Quem | O que faz | Onde |
|---|---|---|
| **Super admin** (você) | Cadastra lojas, liga/desliga módulos, define o que cada dono pode personalizar | App → tela "Lojas" |
| **Dono** | Cardápio, pedidos, equipe e aparência (dentro do que foi liberado) | App → painel da loja |
| **Gerente** | Igual ao dono, menos aparência e cadastro de dono/gerente | App |
| **Atendente / Cozinha** | Pedidos | App → Vendas |
| **Cliente final** | Faz pedidos pelo cardápio | `https://seu-dominio/{endereco-da-loja}/` |

- Toda tabela com dados de loja tem `loja_id` e toda consulta filtra por ele (há testes garantindo que
  uma loja não enxerga dados de outra).
- A personalização (cores, logo, banners/textos, modelo de layout, CSS extra) fica em configuração no
  banco — o código é o mesmo para todas as lojas.
- Não existe cadastro aberto: o super admin cria a loja já com o usuário dono; dono/gerente criam a equipe.

## Rodando localmente

Pré-requisitos: Docker. (Para rodar o app fora do Docker: JDK 17+ e, para Android, o Android Studio.)

```bash
cp .env.exemplo .env          # preencha DB_SENHA, JWT_SEGREDO e SUPERADMIN_*
docker compose up -d --build  # sobe PostgreSQL + servidor em http://localhost:8081
```

Na primeira subida o servidor cria o super admin com `SUPERADMIN_EMAIL`/`SUPERADMIN_SENHA`.
Depois:

1. Abra o painel (desktop: `cd LDLanchesASW && ./gradlew :composeApp:run`) e entre como super admin.
2. Crie uma loja (ex.: endereço `ld-lanches`) com o e-mail e a senha do dono.
3. Entre com o usuário do dono e cadastre categorias e produtos.
4. O cardápio fica em http://localhost:8081/ld-lanches/

### Painel web e Android

```bash
cd LDLanchesASW
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # painel web em http://localhost:8080
./gradlew :composeApp:assembleDebug                 # APK (emulador acessa o servidor em 10.0.2.2:8081)
```

## Funcionamento e encomendas (Fase 1)

- **Horários**: por dia da semana, vários intervalos por dia; fechar antes de abrir = vira a noite
  (ex.: 18:00 às 02:00). Sem horários cadastrados a loja fica sempre aberta.
- **Pausa**: o dono pausa a loja com uma mensagem ("Voltamos segunda!"); nenhum pedido entra.
- **Encomenda agendada** (exige o módulo "Pedido agendado", ligado pelo super admin): o cliente escolhe
  dia e horário dentro do funcionamento, respeitando a antecedência mínima e o limite de pedidos por dia.
  A loja pode trabalhar *só* por encomenda (todo pedido precisa de data).
- **Limite diário**: hoje conta *pedidos*; quando os pedidos tiverem itens estruturados (Fase 2) pode
  passar a contar unidades.
- **Imagens** enviadas ficam em `MIDIA_DIR` (volume `midia` no Docker) e são servidas em `/midia/...`.

## Rodando os testes

Os testes do servidor são de integração e usam um PostgreSQL real (banco `cardapio_teste`, criado
automaticamente pelo `docker compose` na primeira subida):

```bash
cd LDLanchesASW
DB_URL_TESTE=jdbc:postgresql://localhost:5432/cardapio_teste DB_SENHA_TESTE=<sua DB_SENHA> ./gradlew :shared:jvmTest :server:test
```

## API (resumo)

| Rota | Acesso |
|---|---|
| `POST /api/auth/login`, `GET /api/auth/eu`, `PUT /api/auth/eu/senha` | login / usuário logado |
| `/api/admin/lojas` | super admin |
| `/api/painel/{loja,loja/informacoes,usuarios,categorias,produtos,pedidos,dashboard,depoimentos}` | usuários da loja (por papel) |
| `POST /api/painel/arquivos` (multipart, campo `arquivo`) | envio de imagem (PNG/JPG/WEBP, até 5 MB) → `/midia/...` |
| `/api/public/lojas/{slug}` (+ `/categorias`, `/produtos`, `/pedidos`, `/agenda`, `/depoimentos`) | público (cardápio) |

Erros sempre voltam como `{"mensagem": "...", "codigo": "..."}` com a mensagem pronta para o usuário.

## Roteiro

- [x] **Fase 0 — Fundação**: Ktor no lugar do Spring Boot, PostgreSQL + Flyway, multi-loja, perfis de
  acesso, super admin, personalização por loja, IoT removido.
- [x] **Fase 1 — Cardápio completo**: fotos (upload em disco), grupos de opções com mín./máx.,
  preço promocional, produto pausado, horários e pausa da loja, encomenda agendada com antecedência e
  limite de pedidos por dia, contato/endereço/Instagram e depoimentos.
- [ ] **Fase 2 — Pedidos de verdade**: itens estruturados, preço calculado no servidor, áreas de entrega,
  tempo real (WebSocket), tela da cozinha, impressão.
- [ ] **Fase 3 — Mercado Pago**: Pix e cartão, webhooks, estorno.
- [ ] **Fase 4 — Gestão**: caixa, estoque/ficha técnica, entregadores, cupons, fidelidade.
- [ ] **Fase 5 — Relatórios**.
- [ ] **Depois — WhatsApp**.
