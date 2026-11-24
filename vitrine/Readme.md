# Vitrine web (pública) — Tenviti

Vitrine online (cardápio/catálogo) que o cliente final usa para fazer pedidos. É servido pelo servidor Ktor em
`/{endereco-da-loja}/` (ex.: `http://localhost:8081/thai-cookies/`): o primeiro trecho do caminho
identifica a loja, e todos os dados vêm de `/api/public/lojas/{slug}`.

Sacola e histórico de pedidos ficam no `localStorage` separados por loja.

Integrantes do projeto original: José Henrique, Davi e David Braga.

Instruções de execução e visão geral: ver o [README da raiz](../README.md).

## Aparência por loja

O `script.js` aplica a aparência cadastrada (cores, logo, banner, textos, CSS extra). Se a cor de
fundo da loja for clara, o cardápio troca para o tema claro (`css/tema-loja.css`); a cor do texto
sobre botões é escolhida automaticamente para manter contraste.

`lojas/<slug>/` guarda logo e banner das lojas de exemplo (Thai Cookies e LD Lanches), usados pelos dados de exemplo do servidor;
as lojas reais enviam as imagens pelo painel.
