# Tenviti

> **Tem. Vi. Tive.** A loja tem, o cliente vê, o pedido acontece.

Plataforma para **microempreendedores** venderem e atenderem pela internet: vitrine online, pedidos,
encomendas e (em breve) serviços com hora marcada. É um **SaaS multi-loja**: cada loja tem sua vitrine,
sua equipe, seus pedidos e sua própria aparência, e liga só os módulos que usa.

## Estrutura do repositório

```
vitrine/              Vitrine pública (HTML/CSS/JS leve) — servida pelo servidor em /{loja}/
  lojas/<slug>/       Logo e banner das lojas de exemplo
tenviti/
  shared/             Contratos da API (DTOs, enums, formatos) — Kotlin Multiplatform
  server/             Backend Ktor + PostgreSQL (Exposed + Flyway)
  composeApp/         Painel de gestão (Android, Desktop e Web) — Compose Multiplatform
infra/postgres/       Scripts de inicialização do PostgreSQL local
docker-compose.yml    PostgreSQL + servidor (+ painel web com --profile painel)
.env.exemplo          Variáveis de ambiente (copie para .env)
```

## Como funciona o multi-loja

| Quem | O que faz | Onde |
|---|---|---|
| **Super admin** (você) | Cadastra lojas, liga/desliga módulos, define o que cada dono pode personalizar | App → tela "Lojas" |
| **Dono** | Cardápio, pedidos, equipe e aparência (dentro do que foi liberado) | App → painel da loja |
| **Gerente** | Igual ao dono, menos aparência e cadastro de dono/gerente | App |
| **Atendente / Cozinha** | Pedidos | App → Vendas |
| **Cliente final** | Faz pedidos pela vitrine | `https://seu-dominio/{endereco-da-loja}/` |

- Toda tabela com dados de loja tem `loja_id` e toda consulta filtra por ele (há testes garantindo que
  uma loja não enxerga dados de outra).
- A personalização (cores, logo, banners/textos, modelo de layout, CSS extra) fica em configuração no
  banco — o código é o mesmo para todas as lojas.
- Não existe cadastro aberto: o super admin cria a loja já com o usuário dono; dono/gerente criam a equipe.

## Rodando localmente

Pré-requisitos: Docker. (Para rodar o app fora do Docker: JDK 17+ e, para Android, o Android Studio.)

```bash
cp .env.exemplo .env          # preencha DB_SENHA, JWT_SEGREDO, SUPERADMIN_* e os donos de exemplo
docker compose up -d --build  # sobe PostgreSQL + servidor em http://localhost:8081
```

Na primeira subida o servidor cria o super admin com `SUPERADMIN_EMAIL`/`SUPERADMIN_SENHA`.
Com `DADOS_EXEMPLO=true` ele também cria as **lojas de exemplo** (dados fictícios), só se ainda não
existirem:

| Loja | Vitrine | Dono (no `.env`) |
|---|---|---|
| Thai Cookies (encomenda de cookies, horários, boxes "escolha N sabores", depoimentos) | http://localhost:8081/thai-cookies/ | `THAI_DONO_EMAIL` / `THAI_DONO_SENHA` |
| LD Lanches (lanchonete com adicionais) | http://localhost:8081/ld-lanches/ | `LD_DONO_EMAIL` / `LD_DONO_SENHA` |

Para recomeçar do zero: `docker compose down -v && docker compose up -d` (apaga o banco local e as
imagens enviadas; as lojas de exemplo são recriadas).

### Painel web e Android

```bash
cd tenviti
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # painel web em http://localhost:8080
# ou: gere com :composeApp:wasmJsBrowserDevelopmentExecutableDistribution e sirva com
#     docker compose --profile painel up -d
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

## Pedidos (Fase 2)

- A vitrine manda só **ids e quantidades** (produto, opções escolhidas). Nome e preço de cada item
  vêm do catálogo, e o **total é calculado no servidor**: promoção, opções pagas, taxa de entrega.
- O produto confere as próprias regras (disponível, opções que existem, mínimo/máximo de cada grupo,
  repetição). A loja decide a forma de receber (módulos ENTREGA/RETIRADA), cobra a **taxa de entrega
  fixa** e exige o **pedido mínimo** (ambos em "Loja e horários" no painel).
- O pedido guarda uma "foto" dos itens: se o preço mudar depois, o pedido continua com o valor da compra.
- Se o total mudou enquanto o cliente montava a sacola, o pedido é recusado (`total_alterado`) para
  ele conferir o novo valor.
- Pedidos antigos (só texto) continuam aparecendo no painel.

## Rodando os testes

Os testes do servidor são de integração e usam um PostgreSQL real (banco `tenviti_teste`, criado
automaticamente pelo `docker compose` na primeira subida):

```bash
cd tenviti
DB_URL_TESTE=jdbc:postgresql://localhost:5432/tenviti_teste DB_SENHA_TESTE=<sua DB_SENHA> ./gradlew :shared:jvmTest :server:test
```

## API (resumo)

| Rota | Acesso |
|---|---|
| `POST /api/auth/login`, `GET /api/auth/eu`, `PUT /api/auth/eu/senha` | login / usuário logado |
| `/api/admin/lojas` | super admin |
| `/api/painel/{loja,loja/informacoes,usuarios,categorias,produtos,pedidos,dashboard,depoimentos}` | usuários da loja (por papel) |
| `POST /api/painel/arquivos` (multipart, campo `arquivo`) | envio de imagem (PNG/JPG/WEBP, até 5 MB) → `/midia/...` |
| `/api/public/lojas/{slug}` (+ `/categorias`, `/produtos`, `/pedidos`, `/agenda`, `/depoimentos`) | público (vitrine) |

Erros sempre voltam como `{"mensagem": "...", "codigo": "..."}` com a mensagem pronta para o usuário.

## Roteiro

- [x] **Fase 0 — Fundação**: Ktor no lugar do Spring Boot, PostgreSQL + Flyway, multi-loja, perfis de
  acesso, super admin, personalização por loja, IoT removido.
- [x] **Fase 1 — Cardápio completo**: fotos (upload em disco), grupos de opções com mín./máx.,
  preço promocional, produto pausado, horários e pausa da loja, encomenda agendada com antecedência e
  limite de pedidos por dia, contato/endereço/Instagram e depoimentos.
- [ ] **Fase 2 — Tenviti**:
  - [x] Bloco 1: nome Tenviti em todo o projeto, lojas de exemplo recriáveis
  - [x] Bloco 2: pedido com itens estruturados e preço calculado no servidor, dados do cliente,
    taxa de entrega fixa e pedido mínimo
  - [ ] Bloco 3: serviços com hora marcada (duração, horários livres, agendamento)
  - [ ] Bloco 4: convite, planos (Básico/Pró) e 1º mês grátis, cobrança manual
  - [x] Bloco 5: painel com a identidade Tenviti (petróleo e coral, modo escuro, tela "Hoje", pedidos em quadro,
    catálogo em cartões, menu lateral no computador e inferior no celular)
  - [ ] Bloco 6: página do Tenviti
  - [ ] Bloco 7: vitrine nova (mobile-first, mantendo as cores de cada loja)
- [ ] **Fase 3 — Mercado Pago**: Pix e cartão, webhooks, estorno, cobrança automática das lojas;
  tempo real (WebSocket), tela da cozinha, impressão, áreas de entrega.
- [ ] **Fase 4 — Gestão**: caixa, estoque/ficha técnica, entregadores, cupons, fidelidade.
- [ ] **Fase 5 — Relatórios**.
- [ ] **Depois — WhatsApp**.
