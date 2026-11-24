-- Fase 1: cardápio completo, dados e funcionamento da loja, encomendas e depoimentos.

-- ---------- Produto: foto, promoção e grupos de opções ----------
alter table produto
    add column imagem_url varchar(500),
    add column preco_promocional_centavos bigint check (preco_promocional_centavos > 0);

create table grupo_opcao (
    id               bigserial primary key,
    loja_id          bigint      not null references loja (id) on delete cascade,
    produto_id       bigint      not null references produto (id) on delete cascade,
    nome             varchar(80) not null,
    minimo           int         not null default 0 check (minimo >= 0),
    maximo           int         not null default 1 check (maximo >= 1),
    permite_repetir  boolean     not null default false,
    ordem            int         not null default 0,
    constraint grupo_opcao_minimo_ate_maximo check (minimo <= maximo)
);
create index grupo_opcao_produto on grupo_opcao (produto_id);

create table opcao (
    id              bigserial primary key,
    loja_id         bigint      not null references loja (id) on delete cascade,
    grupo_id        bigint      not null references grupo_opcao (id) on delete cascade,
    nome            varchar(80) not null,
    preco_centavos  bigint      not null default 0 check (preco_centavos >= 0),
    ativo           boolean     not null default true,
    ordem           int         not null default 0
);
create index opcao_grupo on opcao (grupo_id);

-- Converte o antigo campo texto "Bacon:2,00; Ovo:1,50" em um grupo "Adicionais"
insert into grupo_opcao (loja_id, produto_id, nome, minimo, maximo, permite_repetir, ordem)
select loja_id, id, 'Adicionais', 0, 10, true, 0
from produto
where trim(adicionais) <> '';

insert into opcao (loja_id, grupo_id, nome, preco_centavos, ativo, ordem)
select g.loja_id,
       g.id,
       left(trim(split_part(t.item, ':', 1)), 80),
       case
           when trim(split_part(t.item, ':', 2)) ~ '^[0-9]{1,3}(\.[0-9]{3})*,[0-9]{1,2}$'
               then round(replace(replace(trim(split_part(t.item, ':', 2)), '.', ''), ',', '.')::numeric * 100)::bigint
           when trim(split_part(t.item, ':', 2)) ~ '^[0-9]+([.,][0-9]{1,2})?$'
               then round(replace(trim(split_part(t.item, ':', 2)), ',', '.')::numeric * 100)::bigint
           else 0
       end,
       true,
       t.ordem::int
from grupo_opcao g
join produto p on p.id = g.produto_id
cross join lateral regexp_split_to_table(p.adicionais, ';') with ordinality as t(item, ordem)
where trim(split_part(t.item, ':', 1)) <> '';

alter table produto drop column adicionais;

-- ---------- Loja: contato, endereço, funcionamento e encomendas ----------
alter table loja
    add column telefone                       varchar(20),
    add column instagram                      varchar(60),
    add column endereco_logradouro            varchar(150),
    add column endereco_numero                varchar(20),
    add column endereco_complemento           varchar(100),
    add column endereco_bairro                varchar(80),
    add column endereco_cidade                varchar(80),
    add column endereco_uf                    varchar(2),
    add column endereco_cep                   varchar(9),
    add column pausada                        boolean not null default false,
    add column mensagem_pausa                 varchar(200),
    add column aceita_agendamento             boolean not null default false,
    add column exige_agendamento              boolean not null default false,
    add column antecedencia_minima_horas      int     not null default 0  check (antecedencia_minima_horas >= 0),
    add column dias_maximos_agendamento       int     not null default 14 check (dias_maximos_agendamento between 1 and 90),
    add column limite_pedidos_dia             int              check (limite_pedidos_dia > 0),
    add column intervalo_agendamento_minutos  int     not null default 30 check (intervalo_agendamento_minutos between 5 and 240);

create table horario_funcionamento (
    id          bigserial primary key,
    loja_id     bigint   not null references loja (id) on delete cascade,
    dia_semana  smallint not null check (dia_semana between 1 and 7), -- 1 = segunda … 7 = domingo
    abre        time     not null,
    fecha       time     not null
);
create index horario_funcionamento_loja on horario_funcionamento (loja_id);

-- ---------- Pedido: encomenda e dia de produção (para o limite diário) ----------
alter table pedido
    add column agendado_para timestamptz,
    add column dia_producao  date;

update pedido p
set dia_producao = (p.criado_em at time zone l.fuso_horario)::date
from loja l
where l.id = p.loja_id;

alter table pedido alter column dia_producao set not null;
create index pedido_loja_dia_producao on pedido (loja_id, dia_producao);

-- ---------- Depoimentos ----------
create table depoimento (
    id       bigserial primary key,
    loja_id  bigint       not null references loja (id) on delete cascade,
    autor    varchar(80)  not null,
    texto    varchar(500) not null,
    nota     smallint     check (nota between 1 and 5),
    ativo    boolean      not null default true,
    ordem    int          not null default 0
);
create index depoimento_loja on depoimento (loja_id);
