-- Fase 2, bloco 2: pedido com itens estruturados, dados do cliente, entrega e pagamento.
-- Pedidos antigos continuam válidos: só têm a descrição em texto (colunas novas ficam nulas).

-- ---------- Loja: taxa de entrega e pedido mínimo ----------
alter table loja
    add column taxa_entrega_centavos  bigint not null default 0 check (taxa_entrega_centavos >= 0),
    add column pedido_minimo_centavos bigint not null default 0 check (pedido_minimo_centavos >= 0);

-- ---------- Pedido: cliente, recebimento, pagamento ----------
alter table pedido
    add column cliente_nome          varchar(80),
    add column cliente_telefone      varchar(13),
    add column tipo_entrega          varchar(20),
    add column endereco_logradouro   varchar(150),
    add column endereco_numero       varchar(20),
    add column endereco_complemento  varchar(100),
    add column endereco_bairro       varchar(80),
    add column endereco_cidade       varchar(80),
    add column endereco_uf           varchar(2),
    add column endereco_cep          varchar(8),
    add column taxa_entrega_centavos bigint not null default 0 check (taxa_entrega_centavos >= 0),
    add column forma_pagamento       varchar(20),
    add column troco_para_centavos   bigint check (troco_para_centavos > 0),
    add column observacao            varchar(500);

-- ---------- Itens do pedido ("foto" do produto na hora da compra) ----------
create table pedido_item (
    id                       bigserial primary key,
    loja_id                  bigint       not null references loja (id) on delete cascade,
    pedido_id                bigint       not null references pedido (id) on delete cascade,
    -- sem FK: o produto pode ser apagado depois e o pedido continua com o nome e o preço da época
    produto_id               bigint,
    nome                     varchar(120) not null,
    quantidade               int          not null check (quantidade between 1 and 99),
    preco_unitario_centavos  bigint       not null check (preco_unitario_centavos >= 0),
    observacao               varchar(200),
    ordem                    int          not null default 0
);
create index pedido_item_pedido on pedido_item (pedido_id);

create table pedido_item_opcao (
    id              bigserial primary key,
    loja_id         bigint      not null references loja (id) on delete cascade,
    item_id         bigint      not null references pedido_item (id) on delete cascade,
    grupo           varchar(80) not null,
    nome            varchar(80) not null,
    preco_centavos  bigint      not null check (preco_centavos >= 0),
    quantidade      int         not null check (quantidade >= 1),
    ordem           int         not null default 0
);
create index pedido_item_opcao_item on pedido_item_opcao (item_id);
