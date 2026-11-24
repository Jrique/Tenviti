-- Fase 0: estrutura multi-loja (multi-tenant).
-- Regra de ouro: toda tabela com dados de uma loja tem loja_id e toda consulta filtra por ele.

create table loja (
    id                      bigserial primary key,
    slug                    varchar(50)  not null unique,
    nome                    varchar(120) not null,
    ativa                   boolean      not null default true,
    fuso_horario            varchar(50)  not null default 'America/Sao_Paulo',

    -- aparência
    cor_primaria            varchar(7)   not null default '#790001',
    cor_secundaria          varchar(7)   not null default '#FFC107',
    cor_fundo               varchar(7)   not null default '#FFF8F6',
    logo_url                varchar(500),
    banner_url              varchar(500),
    modelo_layout           varchar(20)  not null default 'LISTA',
    mensagem_boas_vindas    varchar(300),
    aviso                   varchar(300),
    css_extra               text,

    -- o que está ligado e o que o dono pode editar sozinho
    modulos                 text[]       not null default '{}',
    personalizacao_liberada text[]       not null default '{}',

    criada_em               timestamptz  not null default now()
);

create table usuario (
    id          bigserial primary key,
    loja_id     bigint       references loja (id) on delete cascade,
    nome        varchar(120) not null,
    email       varchar(160) not null,
    senha_hash  varchar(100) not null,
    papel       varchar(20)  not null,
    ativo       boolean      not null default true,
    criado_em   timestamptz  not null default now(),
    constraint usuario_super_admin_sem_loja check ((papel = 'SUPER_ADMIN') = (loja_id is null))
);
create unique index usuario_email_unico on usuario (lower(email));
create index usuario_loja on usuario (loja_id);

create table categoria (
    id       bigserial primary key,
    loja_id  bigint      not null references loja (id) on delete cascade,
    nome     varchar(80) not null,
    ordem    int         not null default 0
);
create unique index categoria_nome_por_loja on categoria (loja_id, lower(nome));

create table produto (
    id              bigserial primary key,
    loja_id         bigint        not null references loja (id) on delete cascade,
    categoria_id    bigint        references categoria (id) on delete set null,
    nome            varchar(120)  not null,
    descricao       varchar(1000) not null default '',
    preco_centavos  bigint        not null check (preco_centavos > 0),
    adicionais      varchar(1000) not null default '', -- provisório até a Fase 1 (grupos de complementos)
    destaque        boolean       not null default false,
    ativo           boolean       not null default true,
    criado_em       timestamptz   not null default now()
);
create index produto_loja on produto (loja_id);

create table pedido (
    id              bigserial primary key,
    loja_id         bigint      not null references loja (id) on delete cascade,
    codigo          varchar(20) not null,
    descricao       text        not null,
    total_centavos  bigint      not null check (total_centavos > 0),
    status          varchar(30) not null,
    criado_em       timestamptz not null default now(),
    atualizado_em   timestamptz not null default now(),
    constraint pedido_codigo_por_loja unique (loja_id, codigo)
);
create index pedido_loja_data on pedido (loja_id, criado_em desc);
