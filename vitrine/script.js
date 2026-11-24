// ================== LOJA (multi-loja) ==================
// O cardápio é servido pelo servidor em /{endereco-da-loja}/ (ex.: /thai-cookies/).
// O primeiro trecho do caminho identifica a loja; tudo vem da API pública dela.
const SLUG_LOJA = (window.location.pathname.split('/').filter(Boolean)[0] || '').toLowerCase();
const API_LOJA = `/api/public/lojas/${encodeURIComponent(SLUG_LOJA)}`;
let VITRINE_LOJA = null;

// Várias lojas ficam no mesmo domínio, então sacola e histórico são separados por loja.
// (O perfil do cliente — nome/endereço — é compartilhado: é a mesma pessoa.)
const CHAVE_CARRINHO = `carrinho:${SLUG_LOJA}`;
const CHAVE_PEDIDOS = `pedidos:${SLUG_LOJA}`;

// Promessa da vitrine: quem precisa da aparência (ex.: imagem padrão dos produtos) espera por ela.
let VITRINE_PRONTA = Promise.resolve(null);
document.addEventListener('DOMContentLoaded', () => { VITRINE_PRONTA = carregarVitrineDaLoja(); });

/** Busca nome, aparência e módulos da loja e aplica no visual do cardápio. */
async function carregarVitrineDaLoja() {
    try {
        const resp = await fetch(API_LOJA, { headers: { 'Accept': 'application/json' } });
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
        VITRINE_LOJA = await resp.json();
        aplicarAparenciaDaLoja(VITRINE_LOJA);
        atualizarStatusLoja();
        if (!numeroWhatsappDaLoja()) {
            const fab = document.getElementById('btn-whats-flutuante');
            if (fab) fab.style.display = 'none';
        }
        if (typeof preencherVisualPerfilGlobal === 'function') preencherVisualPerfilGlobal();
        renderizarSobreLoja();
        carregarDepoimentos();
        ajustarFormasDeEntrega();
        inicializarAgendamentoCheckout();
        return VITRINE_LOJA;
    } catch (e) {
        console.error('Erro ao carregar dados da loja:', e);
        return null;
    }
}

/** Imagem usada nos produtos enquanto não há fotos cadastradas (Fase 1): o logo da loja. */
function imagemPadraoProduto() {
    return (VITRINE_LOJA && VITRINE_LOJA.aparencia && VITRINE_LOJA.aparencia.logoUrl) || 'images/loja-padrao.svg';
}

// ---------- Cores: contraste automático ----------
function hexParaRgb(hex) {
    const n = parseInt(hex.replace('#', ''), 16);
    return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}
function rgbParaHex([r, g, b]) {
    return '#' + [r, g, b].map(v => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')).join('');
}
function luminancia(hex) {
    const [r, g, b] = hexParaRgb(hex).map(v => {
        const c = v / 255;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    });
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}
function contraste(a, b) {
    const [l1, l2] = [luminancia(a), luminancia(b)].sort((x, y) => y - x);
    return (l1 + 0.05) / (l2 + 0.05);
}
/** Escolhe texto claro ou escuro, o que tiver mais contraste com a cor de fundo. */
function textoSobre(cor) {
    return contraste(cor, '#FFFFFF') >= contraste(cor, '#1E2B36') ? '#FFFFFF' : '#1E2B36';
}
/** Escurece a cor até atingir o contraste mínimo (WCAG 4.5) contra o fundo. */
function corLegivelSobre(cor, fundo, minimo = 4.5) {
    let rgb = hexParaRgb(cor);
    for (let i = 0; i < 30 && contraste(rgbParaHex(rgb), fundo) < minimo; i++) rgb = rgb.map(v => v * 0.9);
    return rgbParaHex(rgb);
}

function aplicarAparenciaDaLoja(loja) {
    const ap = loja.aparencia || {};
    const raiz = document.documentElement;
    const css = raiz.style;
    const temaClaro = ap.corFundo && luminancia(ap.corFundo) > 0.5;

    if (ap.corPrimaria) {
        css.setProperty('--cor-primaria-start', ap.corPrimaria);
        css.setProperty('--cor-texto-sobre-primaria', textoSobre(ap.corPrimaria));
        if (temaClaro) css.setProperty('--cor-primaria-end', corLegivelSobre(ap.corPrimaria, '#FFFFFF', 2.2));
    }
    if (ap.corSecundaria) {
        css.setProperty('--cor-acento', ap.corSecundaria);
        css.setProperty('--cor-texto-sobre-acento', textoSobre(ap.corSecundaria));
        css.setProperty('--cor-acento-texto', temaClaro ? corLegivelSobre(ap.corSecundaria, '#FFFFFF') : ap.corSecundaria);
    }
    if (temaClaro) {
        raiz.dataset.tema = 'claro';
        css.setProperty('--cor-fundo', ap.corFundo);
    }

    if (loja.nome) {
        document.title = document.title.replace('Tenviti', loja.nome);
        document.querySelectorAll('.logo-text').forEach(el => { el.textContent = loja.nome; });
    }
    if (ap.logoUrl) {
        document.querySelectorAll('img[src="images/loja-padrao.svg"]').forEach(img => {
            img.src = ap.logoUrl;
            img.alt = `Logo ${loja.nome}`;
        });
    }

    // Faixa de boas-vindas (hero): banner e texto da loja
    const hero = document.querySelector('.hero-banner');
    if (hero) {
        if (ap.bannerUrl) {
            const veu = temaClaro ? 'rgba(255,255,255,0.55), rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.6), rgba(0,0,0,0.6)';
            hero.style.backgroundImage = `linear-gradient(90deg, ${veu}), url('${ap.bannerUrl}')`;
            hero.classList.add('hero-com-banner');
        }
        if (ap.mensagemBoasVindas) {
            const [titulo, ...resto] = ap.mensagemBoasVindas.split('\n');
            const h2 = hero.querySelector('h2');
            const p = hero.querySelector('p');
            if (h2) h2.textContent = titulo;
            if (p) p.textContent = resto.join(' ').trim();
        }
    }

    if (ap.aviso) {
        const faixa = document.createElement('div');
        faixa.className = 'aviso-loja';
        faixa.textContent = ap.aviso;
        faixa.style.cssText = 'background:var(--cor-acento);color:var(--cor-texto-sobre-acento);text-align:center;padding:8px 12px;font-weight:600;';
        document.body.prepend(faixa);
    }
    if (ap.cssExtra) {
        const estilo = document.createElement('style');
        estilo.textContent = ap.cssExtra;
        document.head.appendChild(estilo);
    }
}

/** Converte a data ISO do servidor em "Hoje, 15:30" / "Ontem, 09:10" / "12/11/2026, 20:00". */
function formatarDataPedido(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    const hoje = new Date();
    const ontem = new Date(); ontem.setDate(hoje.getDate() - 1);
    const mesmoDia = (a, b) => a.toDateString() === b.toDateString();
    const hora = d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    if (mesmoDia(d, hoje)) return `Hoje, ${hora}`;
    if (mesmoDia(d, ontem)) return `Ontem, ${hora}`;
    return `${d.toLocaleDateString('pt-BR')}, ${hora}`;
}

// ================== CONFIG GERAL ==================
let categorias = [];
const mockProdutos = [];

// ---------- Dados da loja (vêm de /api/public/lojas/{slug}) ----------
function contatoDaLoja() {
    return (VITRINE_LOJA && VITRINE_LOJA.contato) || {};
}
function formatarTelefone(t) {
    if (!t) return '';
    const d = String(t).replace(/\D/g, '');
    if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
    if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
    return d;
}
/** Endereço da loja no formato usado pelas telas (retirada no balcão). */
function enderecoDaLoja() {
    const c = contatoDaLoja();
    return { nome: (VITRINE_LOJA && VITRINE_LOJA.nome) || '', ...(c.endereco || {}), telefone: formatarTelefone(c.telefone) };
}
/** Número no formato do wa.me (55 + DDD + número) ou null se a loja não cadastrou telefone. */
function numeroWhatsappDaLoja() {
    const t = contatoDaLoja().telefone;
    return t ? '55' + t : null;
}

/** Escapa texto vindo do cadastro antes de inserir como HTML. */
function esc(texto) {
    return String(texto ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** Seção "Sobre a loja" da página inicial: horários, endereço, telefone e Instagram. */
function renderizarSobreLoja() {
    const secao = document.getElementById('secao-sobre-loja');
    const alvo = document.getElementById('sobre-loja-conteudo');
    if (!secao || !alvo || !VITRINE_LOJA) return;

    const c = contatoDaLoja();
    const e = c.endereco;
    const linhas = [];

    const horarios = VITRINE_LOJA.horarios || [];
    if (horarios.length) {
        const porDia = {};
        horarios.forEach(h => { (porDia[h.diaSemana] = porDia[h.diaSemana] || []).push(`${h.abre}–${h.fecha}`); });
        const dias = [1, 2, 3, 4, 5, 6, 7].map(d =>
            `<div class="horario-linha"><span>${DIAS_SEMANA[d][0].toUpperCase() + DIAS_SEMANA[d].slice(1)}</span><span>${porDia[d] ? porDia[d].join(', ') : 'Fechado'}</span></div>`);
        linhas.push(`<div class="sobre-bloco"><strong>Horários</strong>${dias.join('')}</div>`);
    }
    if (e) {
        const l1 = [e.logradouro, e.numero].filter(Boolean).join(', ');
        const l2 = [e.bairro, [e.cidade, e.uf].filter(Boolean).join('/')].filter(Boolean).join(' - ');
        linhas.push(`<div class="sobre-bloco"><strong>Endereço</strong><div>${esc(l1)}${e.complemento ? ' · ' + esc(e.complemento) : ''}</div><div>${esc(l2)}</div></div>`);
    }
    const contatos = [];
    if (c.telefone) contatos.push(`<div>📞 ${esc(formatarTelefone(c.telefone))}</div>`);
    if (c.instagram) contatos.push(`<div>📷 <a href="https://instagram.com/${encodeURIComponent(c.instagram)}" target="_blank" rel="noopener">@${esc(c.instagram)}</a></div>`);
    if (contatos.length) linhas.push(`<div class="sobre-bloco"><strong>Contato</strong>${contatos.join('')}</div>`);

    alvo.innerHTML = linhas.join('');
    secao.hidden = linhas.length === 0;
}

/** Depoimentos ativos da loja (página inicial). */
async function carregarDepoimentos() {
    const secao = document.getElementById('secao-depoimentos');
    const lista = document.getElementById('depoimentos-lista');
    if (!secao || !lista) return;
    try {
        const resp = await fetch(`${API_LOJA}/depoimentos`, { headers: { 'Accept': 'application/json' } });
        if (!resp.ok) return;
        const depoimentos = await resp.json();
        lista.innerHTML = depoimentos.map(d => `
            <div class="depoimento-card">
                ${d.nota ? `<div class="depoimento-nota" aria-label="Nota ${d.nota} de 5">${'★'.repeat(d.nota)}${'☆'.repeat(5 - d.nota)}</div>` : ''}
                <p class="depoimento-texto">“${esc(d.texto)}”</p>
                <div class="depoimento-autor">— ${esc(d.autor)}</div>
            </div>`).join('');
        secao.hidden = depoimentos.length === 0;
    } catch (e) {
        console.error('Erro ao carregar depoimentos:', e);
    }
}

// ---------- Checkout: modo de entrega e encomenda agendada ----------
let AGENDA_LOJA = [];

/** Mostra só as formas de entrega ligadas para a loja (módulos ENTREGA / RETIRADA). */
function ajustarFormasDeEntrega() {
    if (!VITRINE_LOJA) return;
    const modulos = VITRINE_LOJA.modulos || [];
    const radios = document.querySelectorAll('input[name="tipoEntrega"]');
    if (!radios.length) return;
    radios.forEach(r => {
        const ligado = modulos.includes(r.value);
        r.closest('label').style.display = ligado ? '' : 'none';
        if (!ligado && r.checked) r.checked = false;
    });
    if (!document.querySelector('input[name="tipoEntrega"]:checked')) {
        const primeiro = Array.from(radios).find(r => modulos.includes(r.value));
        if (primeiro) { primeiro.checked = true; primeiro.dispatchEvent(new Event('change', { bubbles: true })); }
    }
}

function rotuloDia(iso) {
    const [a, m, d] = iso.split('-').map(Number);
    const data = new Date(a, m - 1, d);
    const hoje = new Date(); hoje.setHours(0, 0, 0, 0);
    const dif = Math.round((data - hoje) / 86400000);
    const semana = data.toLocaleDateString('pt-BR', { weekday: 'long' });
    const dm = `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}`;
    if (dif === 0) return `Hoje (${dm})`;
    if (dif === 1) return `Amanhã (${dm})`;
    return `${semana[0].toUpperCase() + semana.slice(1)} (${dm})`;
}

/** Preenche dia/horário da encomenda a partir de /agenda. */
async function inicializarAgendamentoCheckout() {
    const secao = document.getElementById('checkout-agendamento');
    if (!secao || !VITRINE_LOJA) return;
    const enc = VITRINE_LOJA.encomenda || {};
    if (!enc.aceitaAgendamento) return;

    try {
        const resp = await fetch(`${API_LOJA}/agenda`, { headers: { 'Accept': 'application/json' } });
        AGENDA_LOJA = resp.ok ? await resp.json() : [];
    } catch (e) {
        AGENDA_LOJA = [];
    }

    const selDia = document.getElementById('agendamento-dia');
    const selHora = document.getElementById('agendamento-hora');
    const nota = document.getElementById('agendamento-nota');

    const opcoesDia = [];
    if (!enc.exigeAgendamento && VITRINE_LOJA.aberta) opcoesDia.push('<option value="">O quanto antes</option>');
    AGENDA_LOJA.forEach(d => opcoesDia.push(
        `<option value="${d.data}" ${d.lotado ? 'disabled' : ''}>${rotuloDia(d.data)}${d.lotado ? ' — sem vagas' : ''}</option>`));
    selDia.innerHTML = opcoesDia.join('');

    // primeiro dia disponível já selecionado
    const primeiroLivre = Array.from(selDia.options).find(o => !o.disabled);
    if (primeiroLivre) selDia.value = primeiroLivre.value;

    function preencherHoras() {
        const dia = AGENDA_LOJA.find(d => d.data === selDia.value);
        selHora.innerHTML = dia ? dia.horarios.map(h => `<option value="${h}">${h}</option>`).join('') : '';
        selHora.parentElement.style.display = dia ? '' : 'none';
    }
    selDia.addEventListener('change', preencherHoras);
    preencherHoras();

    const avisos = [];
    if (enc.antecedenciaMinimaHoras > 0) avisos.push(`Pedidos com pelo menos ${enc.antecedenciaMinimaHoras}h de antecedência.`);
    if (!AGENDA_LOJA.some(d => !d.lotado)) avisos.push('No momento não há datas disponíveis.');
    nota.textContent = avisos.join(' ');
    secao.hidden = false;
}

/** Data/hora escolhida para a encomenda ("AAAA-MM-DDTHH:mm") ou null para "o quanto antes". */
function agendamentoEscolhido() {
    const secao = document.getElementById('checkout-agendamento');
    if (!secao || secao.hidden) return null;
    const dia = document.getElementById('agendamento-dia').value;
    const hora = document.getElementById('agendamento-hora').value;
    return dia && hora ? `${dia}T${hora}` : null;
}

/** Valor total de uma linha da sacola: (preço do produto + opções escolhidas) × quantidade. */
function valorItem(item) {
    const opcoes = (item.extras || []).reduce((soma, e) => soma + (e.preco * e.qtd), 0);
    return (item.precoBase + opcoes) * item.quantidade;
}

async function carregarProdutosDoBackend() {
    try {
        const resp = await fetch(`${API_LOJA}/produtos`, {
            headers: { 'Accept': 'application/json' }
        });

        if (!resp.ok) {
            throw new Error(`Erro HTTP: ${resp.status}`);
        }

        const produtosApi = await resp.json();
        await VITRINE_PRONTA; // garante a imagem padrão da loja

        // esvazia a lista atual
        mockProdutos.length = 0;

        // converte o DTO da API para o formato usado no front
        // ProdutoDto do servidor (preços em centavos; grupos = opções com mínimo/máximo)
        produtosApi.forEach(p => {
            const temPromo = p.precoPromocionalCentavos != null;
            mockProdutos.push({
                id: p.id,
                nome: p.nome,
                descricao: p.descricao || '',
                preco: (temPromo ? p.precoPromocionalCentavos : p.precoCentavos) / 100,
                precoOriginal: temPromo ? p.precoCentavos / 100 : null,
                imagemUrl: p.imagemUrl || imagemPadraoProduto(),
                categoria: p.categoriaNome || 'Outros',
                grupos: (p.grupos || []).map(g => ({
                    nome: g.nome, minimo: g.minimo, maximo: g.maximo, permiteRepetir: g.permiteRepetir,
                    opcoes: (g.opcoes || []).map(o => ({ nome: o.nome, preco: (o.precoCentavos || 0) / 100 }))
                })),
                destaque: !!p.destaque
            });
        });

    } catch (e) {
        console.error('Erro ao carregar produtos do backend:', e);
    }
}

function preencherBannersDestaque() {
    const track = document.getElementById('banners-track');
    if (!track) return;

    const destacados = mockProdutos.filter(p => p.destaque);
    const lista = destacados.length ? destacados : mockProdutos.slice(0, 3);

    track.innerHTML = '';

    lista.forEach(produto => {
        const slide = document.createElement('div');
        slide.className = 'banner-slide';
        slide.innerHTML = `
            <div class="banner-info">
                <div class="banner-label">${produto.destaque ? 'Destaque da casa': 'Mais pedido'}</div>
                <h2 class="banner-titulo banner-titulo-limitado">${produto.nome}</h2>
                <p class="banner-desc banner-desc-limitada">${produto.descricao || ''}</p>
                <div class="banner-preco">
                    <span class="preco-atual">R$ ${produto.preco.toFixed(2).replace('.', ',')}</span>
                </div>
                <button class="banner-btn" onclick="addToCartWithFeedback(${produto.id})">
                    <span>Adicionar à sacola</span>
                </button>
            </div>
            <div class="banner-img">
                <img src="${produto.imagemUrl}" alt="${produto.nome}">
            </div>
        `;
        track.appendChild(slide);
    });
}
async function carregarCategoriasDoBackend() {
    try {
      const resp = await fetch(`${API_LOJA}/categorias`, {
        headers: { 'Accept': 'application/json' }
      });

      if (!resp.ok) {
        console.error('Erro ao carregar categorias', resp.status);
        return;
      }

      categorias = await resp.json(); // [{ id, nome, ordem }]
    } catch (e) {
      console.error('Erro ao carregar categorias', e);
    }
  }

const taxaEntrega = 5.00;


// ================== PERFIL / CADASTRO ==================
function getPerfil() {
    try {
        const data = localStorage.getItem('ldPerfil');
        return data ? JSON.parse(data) : null;
    } catch (e) {
        console.error('Erro lendo perfil:', e);
        return null;
    }
}

function salvarPerfil(dados) {
    try {
        localStorage.setItem('ldPerfil', JSON.stringify(dados));
    } catch (e) {
        console.error('Erro salvando perfil:', e);
    }
}

function apagarPerfil() {
    try {
        localStorage.removeItem('ldPerfil');
    } catch (e) {
        console.error('Erro removendo perfil:', e);
    }
}

function perfilIncompleto(perfil) {
    if (!perfil) return true;
    if (!perfil.nome) return true;
    if (!perfil.telefone) return true;
    if (!perfil.logradouro) return true;
    if (!perfil.numero) return true;
    if (!perfil.bairro) return true;
    if (!perfil.cidade) return true;
    return false;
}

function abrirModalPerfilGlobal() {
    const modal = document.getElementById('modal-perfil');
    if (!modal) {
        console.warn('modal-perfil não existe nesta página');
        return false;
    }
    preencherFormPerfilGlobal();
    registrarEventosCepPerfil();
    modal.style.display = 'flex';
    return true;
}

function fecharModalPerfilGlobal() {
    const modal = document.getElementById('modal-perfil');
    if (!modal) return;
    modal.style.display = 'none';
}

function preencherVisualPerfilGlobal() {
    const dados = getPerfil();

    // Mini perfil (header)
    const nomeEl = document.getElementById('perfil-nome');
    const endEl  = document.getElementById('perfil-endereco');

    if (nomeEl) nomeEl.textContent = dados?.nome || '-';

    const userLinha1 = dados
        ? [dados.logradouro, dados.numero].filter(Boolean).join(', ')
        : '';
    const userLinha2 = dados
        ? [
            dados.bairro,
            dados.cidade && dados.uf ? `${dados.cidade}/${dados.uf}` : (dados.cidade || dados.uf)
        ].filter(Boolean).join(' - ')
        : '';

    if (endEl) {
        if (!dados || perfilIncompleto(dados)) {
            endEl.innerHTML = 'Nenhum endereço cadastrado ainda.';
        } else {
            endEl.innerHTML = `
                ${userLinha1}<br>
                ${userLinha2}<br>
                ${dados.complemento ? dados.complemento + '<br>' : ''}
                ${dados.cep ? 'CEP: ' + dados.cep + '<br>' : ''}
                Tel: ${dados.telefone || '-'}
            `;
        }
    }

    // Bloco de endereço do CHECKOUT
    const entregaBox = document.getElementById('checkout-endereco-dados');
    if (entregaBox) {
        const tipoEntregaSelecionado =
            document.querySelector('input[name="tipoEntrega"]:checked')?.value || 'ENTREGA';

        if (tipoEntregaSelecionado === 'RETIRADA') {
            const loja = enderecoDaLoja();

            const lojaLinha1 = [loja.logradouro, loja.numero].filter(Boolean).join(', ');
            const lojaCidadeUf = (loja.cidade && loja.uf)
                ? `${loja.cidade}/${loja.uf}`
                : (loja.cidade || loja.uf || '');
            const lojaLinha2 = [loja.bairro, lojaCidadeUf].filter(Boolean).join(' - ');

            entregaBox.innerHTML = `
                <div class="endereco-linha"><strong>${loja.nome}</strong></div>
                ${lojaLinha1 ? `<div class="endereco-linha">${lojaLinha1}</div>` : ''}
                ${lojaLinha2 ? `<div class="endereco-linha">${lojaLinha2}</div>` : ''}
                ${loja.complemento ? `<div class="endereco-linha">${loja.complemento}</div>` : ''}
                <div class="endereco-linha">
                    ${loja.cep ? 'CEP: ' + loja.cep + ' - ' : ''}
                    Tel: ${loja.telefone || '-'}
                </div>
            `;
        } else {
        
            if (!dados || perfilIncompleto(dados)) {
                entregaBox.innerHTML = `
                    <div class="checkout-endereco-alerta">
                        Dados incompletos para entrega.<br>
                        Clique em "Editar dados de entrega" antes de finalizar.
                    </div>
                `;
            } else {
                entregaBox.innerHTML = `
                    <div class="endereco-linha"><strong>${dados.nome}</strong></div>
                    <div class="endereco-linha">${userLinha1}</div>
                    <div class="endereco-linha">${userLinha2}</div>
                    <div class="endereco-linha">${dados.complemento || ""}</div>
                    <div class="endereco-linha">
                        ${dados.cep ? 'CEP: ' + dados.cep + ' - ' : ''}
                        Tel: ${dados.telefone}
                    </div>
                `;
            }
        }
    }
}

function preencherFormPerfilGlobal() {
    const dados = getPerfil();

    const inputNome        = document.getElementById('perfilNome');
    const inputCep         = document.getElementById('perfilCep');
    const inputLogradouro  = document.getElementById('perfilLogradouro');
    const inputNumero      = document.getElementById('perfilNumero');
    const inputBairro      = document.getElementById('perfilBairro');
    const inputCidade      = document.getElementById('perfilCidade');
    const inputUf          = document.getElementById('perfilUf');
    const inputTelefone    = document.getElementById('perfilTelefone');
    const inputComplemento = document.getElementById('perfilComplemento');

    if (inputNome)        inputNome.value        = dados?.nome || '';
    if (inputCep)         inputCep.value         = dados?.cep || '';
    if (inputLogradouro)  inputLogradouro.value  = dados?.logradouro || '';
    if (inputNumero)      inputNumero.value      = dados?.numero || '';
    if (inputBairro)      inputBairro.value      = dados?.bairro || '';
    if (inputCidade)      inputCidade.value      = dados?.cidade || '';
    if (inputUf)          inputUf.value          = dados?.uf || '';
    if (inputTelefone)    inputTelefone.value    = dados?.telefone || '';
    if (inputComplemento) inputComplemento.value = dados?.complemento || '';
}

function atualizarResumoEntregaHome() {
    const dados   = getPerfil();
    const textoEl = document.getElementById('home-endereco-text');
    const acaoBtn = document.getElementById('home-endereco-acao');

    if (!textoEl || !acaoBtn) return;

    if (!dados || perfilIncompleto(dados)) {
        textoEl.textContent = "Informe seu endereço para ver o prazo e valor de entrega.";
        acaoBtn.textContent = "Adicionar endereço";
        return;
    }

    const linhaCidadeUf = dados.cidade && dados.uf
        ? `${dados.cidade}/${dados.uf}`
        : dados.cidade || dados.uf || '';

    const complementoLinha = dados.bairro && linhaCidadeUf
        ? `${dados.bairro} - ${linhaCidadeUf}`
        : dados.bairro || linhaCidadeUf || '';

    let texto = `Entregar em: ${dados.logradouro}, ${dados.numero}`;
    if (complementoLinha) {
        texto += ` • ${complementoLinha}`;
    }

    textoEl.textContent = texto;
    acaoBtn.textContent = "Alterar endereço";
}

function salvarPerfilDoModal() {
    const inputNome        = document.getElementById('perfilNome');
    const inputCep         = document.getElementById('perfilCep');
    const inputLogradouro  = document.getElementById('perfilLogradouro');
    const inputNumero      = document.getElementById('perfilNumero');
    const inputBairro      = document.getElementById('perfilBairro');
    const inputCidade      = document.getElementById('perfilCidade');
    const inputUf          = document.getElementById('perfilUf');
    const inputTelefone    = document.getElementById('perfilTelefone');
    const inputComplemento = document.getElementById('perfilComplemento');

    const novo = {
        nome:        (inputNome?.value || '').trim(),
        cep:         (inputCep?.value || '').trim(),
        logradouro:  (inputLogradouro?.value || '').trim(),
        numero:      (inputNumero?.value || '').trim(),
        bairro:      (inputBairro?.value || '').trim(),
        cidade:      (inputCidade?.value || '').trim(),
        uf:          (inputUf?.value || '').trim().toUpperCase(),
        telefone:    (inputTelefone?.value || '').trim(),
        complemento: (inputComplemento?.value || '').trim()
    };

    if (!novo.nome || !novo.telefone || !novo.logradouro || !novo.numero || !novo.bairro || !novo.cidade) {
        showActionFeedback(
            "Atenção",
            "Preencha nome, telefone, rua, número, bairro e cidade para continuar."
        );
        return;
    }

    salvarPerfil(novo);
    preencherVisualPerfilGlobal();
    atualizarResumoEntregaHome();
    fecharModalPerfilGlobal();

    showActionFeedback(
        "Dados salvos",
        "As informações do seu endereço foram salvas. Agora você já pode finalizar o pedido."
    );
}

function excluirPerfilFluxo() {
    showConfirmDelete(
        "Excluir conta",
        "Tem certeza que deseja excluir sua conta? Seus dados de entrega serão removidos.",
        () => {
            apagarPerfil();
            preencherVisualPerfilGlobal();
            preencherFormPerfilGlobal();
            atualizarResumoEntregaHome();
            showActionFeedback(
                "Conta excluída",
                "Seus dados foram removidos deste dispositivo."
            );
        }
    );
}

// ================== VIA CEP ==================
function registrarEventosCepPerfil() {
    const cepInput = document.getElementById('perfilCep');
    if (!cepInput) return;

    if (cepInput._viaCepReg) return;
    cepInput._viaCepReg = true;

    const acionarBusca = () => {
        const valor = cepInput.value.trim();
        if (!valor) return;
        buscarCepEPreencherEndereco(valor);
    };

    cepInput.addEventListener('blur', acionarBusca);
    cepInput.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            acionarBusca();
        }
    });
}

function buscarCepEPreencherEndereco(cepBruto) {
    const cepLimpo = cepBruto.replace(/\D/g, '');

    if (cepLimpo.length !== 8) {
        showActionFeedback(
            "CEP inválido",
            "Digite um CEP válido com 8 números."
        );
        return;
    }

    const url = `https://viacep.com.br/ws/${cepLimpo}/json/`;

    fetch(url)
        .then(r => r.json())
        .then(dados => {
            if (dados.erro) {
                showActionFeedback(
                    "CEP não encontrado",
                    "Não foi possível localizar o CEP informado. Verifique e tente novamente."
                );
                return;
            }

            const cepInput        = document.getElementById('perfilCep');
            const logradouroInput = document.getElementById('perfilLogradouro');
            const bairroInput     = document.getElementById('perfilBairro');
            const cidadeInput     = document.getElementById('perfilCidade');
            const ufInput         = document.getElementById('perfilUf');

            if (cepInput)        cepInput.value        = dados.cep || cepLimpo;
            if (logradouroInput) logradouroInput.value = dados.logradouro || '';
            if (bairroInput)     bairroInput.value     = dados.bairro || '';
            if (cidadeInput)     cidadeInput.value     = dados.localidade || '';
            if (ufInput)         ufInput.value         = (dados.uf || '').toUpperCase();

            showActionFeedback(
                "Endereço encontrado",
                "Preenchemos rua, bairro, cidade e UF com base no CEP. Confira se está tudo certo."
            );
        })
        .catch(() => {
            showActionFeedback(
                "Erro na consulta",
                "Não foi possível consultar o CEP no momento. Verifique sua conexão e tente novamente."
            );
        });
}

// ================== UI GLOBAL ==================
function configurarBotaoHeaderEntrega() {
    const btn = document.getElementById('btn-header-entrega');
    if (!btn) return;

    btn.addEventListener('click', () => {
        abrirModalPerfilGlobal();
    });
}

const DIAS_SEMANA = ['', 'segunda', 'terça', 'quarta', 'quinta', 'sexta', 'sábado', 'domingo'];

/** Próxima abertura a partir de agora (texto curto) ou null. diaSemana: 1 = segunda … 7 = domingo. */
function proximaAbertura(horarios) {
    if (!horarios || !horarios.length) return null;
    const agora = new Date();
    const minAgora = agora.getHours() * 60 + agora.getMinutes();
    for (let i = 0; i < 8; i++) {
        const d = new Date(agora); d.setDate(agora.getDate() + i);
        const dia = d.getDay() === 0 ? 7 : d.getDay();
        const doDia = horarios.filter(h => h.diaSemana === dia)
            .map(h => h.abre)
            .filter(abre => i > 0 || (parseInt(abre.slice(0, 2)) * 60 + parseInt(abre.slice(3))) > minAgora)
            .sort();
        if (doDia.length) {
            if (i === 0) return `abre hoje às ${doDia[0]}`;
            if (i === 1) return `abre amanhã às ${doDia[0]}`;
            return `abre ${DIAS_SEMANA[dia]} às ${doDia[0]}`;
        }
    }
    return null;
}

function atualizarStatusLoja() {
    const flagEl = document.getElementById('loja-status-flag');
    const descEl = document.getElementById('loja-status-desc');
    if (!flagEl || !descEl || !VITRINE_LOJA) return;

    const v = VITRINE_LOJA;
    const encomenda = v.encomenda || {};
    let titulo, desc;
    if (v.pausada) {
        titulo = 'Pausado';
        desc = v.mensagemPausa || 'Não estamos recebendo pedidos no momento.';
    } else if (encomenda.exigeAgendamento) {
        titulo = 'Por encomenda';
        desc = 'Escolha a data e o horário ao finalizar o pedido.';
    } else if (v.aberta) {
        titulo = 'Aberto';
        desc = encomenda.aceitaAgendamento ? 'Peça agora ou agende sua encomenda.' : 'Estamos recebendo pedidos.';
    } else {
        titulo = 'Fechado';
        const prox = proximaAbertura(v.horarios);
        desc = [prox ? prox[0].toUpperCase() + prox.slice(1) + '.' : '', encomenda.aceitaAgendamento ? 'Você pode agendar sua encomenda.' : '']
            .filter(Boolean).join(' ');
    }
    const ok = !v.pausada && (v.aberta || encomenda.aceitaAgendamento);
    flagEl.textContent = titulo;
    flagEl.classList.toggle('status-aberto', ok);
    flagEl.classList.toggle('status-fechado', !ok);
    descEl.textContent = desc;
}

function configurarBotaoWhatsFlutuante() {
    const btn = document.getElementById('btn-whats-flutuante');
    if (!btn) return;

    btn.addEventListener('click', () => {
        const numero = numeroWhatsappDaLoja();
        if (!numero) return;
        const msg = encodeURIComponent("Olá, gostaria de fazer um pedido.");
        window.open("https://wa.me/" + numero + "?text=" + msg, "_blank");
    });
}

function showActionFeedback(titulo, texto) {
    const overlay = document.getElementById('modal-msg');
    const titleEl = document.getElementById('modal-msg-title');
    const textEl  = document.getElementById('modal-msg-text');
    const okBtn   = document.getElementById('modal-msg-ok-btn');
    const closeBtn = document.getElementById('modal-msg-close-btn');

    if (!overlay || !titleEl || !textEl) {
        console.log('FEEDBACK:', titulo, texto);
        return;
    }

    titleEl.textContent = titulo;
    textEl.textContent  = texto;
    overlay.style.display = 'flex';

    function fechar() {
        overlay.style.display = 'none';
    }
    if (okBtn)   okBtn.onclick   = fechar;
    if (closeBtn) closeBtn.onclick = fechar;
}

function showConfirmDelete(titulo, texto, onConfirm) {
    const overlay   = document.getElementById('modal-confirm-delete');
    const titleEl   = document.getElementById('modal-confirm-delete-title');
    const textEl    = document.getElementById('modal-confirm-delete-text');
    const cancelBtn = document.getElementById('modal-confirm-delete-cancel-btn');
    const confirmBtn= document.getElementById('modal-confirm-delete-ok-btn');
    const closeBtn  = document.getElementById('modal-confirm-delete-close-btn');

    if (!overlay || !titleEl || !textEl || !cancelBtn || !confirmBtn) {
        console.warn("Modal de confirmação não encontrado na página.");
        return;
    }

    titleEl.textContent = titulo;
    textEl.textContent  = texto;
    overlay.style.display = 'flex';

    function fechar() {
        overlay.style.display = 'none';
        cancelBtn.onclick = null;
        confirmBtn.onclick = null;
        if (closeBtn) closeBtn.onclick = null;
    }

    cancelBtn.onclick = fechar;
    if (closeBtn) closeBtn.onclick = fechar;

    confirmBtn.onclick = () => {
        fechar();
        if (typeof onConfirm === 'function') onConfirm();
    };
}

// ================== CARRINHO / SACOLA ==================
function getCart() {
    try {
        const data = localStorage.getItem(CHAVE_CARRINHO);
        return data ? JSON.parse(data) : [];
    } catch (e) {
        console.error('Erro lendo carrinho:', e);
        return [];
    }
}

function saveCart(cart) {
    try {
        localStorage.setItem(CHAVE_CARRINHO, JSON.stringify(cart));
    } catch (e) {
        console.error('Erro salvando carrinho:', e);
    }
}

function clearCart() {
    try {
        localStorage.removeItem(CHAVE_CARRINHO);
    } catch (e) {
        console.error('Erro limpando carrinho:', e);
    }
}

/** O produto tem grupo obrigatório (precisa abrir a página do produto para escolher)? */
function produtoExigeEscolha(produto) {
    return (produto.grupos || []).some(g => g.minimo > 0);
}

/**
 * Adiciona um produto à sacola com as opções escolhidas.
 * selecoes: [{ grupo, nome, preco, qtd }] — preço e quantidade por unidade do produto.
 */
function addToCart(produto, selecoes = []) {
    const cart = getCart();
    const extrasDetalhados = (selecoes || []).filter(e => e.qtd > 0);

    // considera dois itens iguais se tiverem o mesmo produto + mesma combinação de extras
    const existente = cart.find(item =>
        item.id === produto.id &&
        JSON.stringify(item.extras || []) === JSON.stringify(extrasDetalhados)
    );

    if (existente) {
        existente.quantidade += 1;
    } else {
        cart.push({
            id: produto.id,
            nome: produto.nome,
            precoBase: produto.preco,
            imagemUrl: produto.imagemUrl,
            quantidade: 1,
            extras: extrasDetalhados   // [{nome, preco, qtd}]
        });
    }
    

    saveCart(cart);
}

/**
 * Versão com feedback visual (toast/modal)
 */
function addToCartWithFeedback(produtoId, selecoes = []) {
    const produto = mockProdutos.find(p => p.id === produtoId);
    if (!produto) return;

    // Produto com escolha obrigatória (ex.: "escolha 4 sabores"): abre a página dele
    if (produtoExigeEscolha(produto) && (!selecoes || selecoes.length === 0)) {
        window.location.href = 'produto.html?id=' + produto.id;
        return;
    }

    addToCart(produto, selecoes);

    const nome = produto.nome || "Item";
    showActionFeedback(
        "Adicionado à sacola",
        nome + " foi adicionado na sua sacola."
    );
    atualizarBadgeCarrinho();
}

function updateCartQuantity(produtoId, action) {
    alterarQuantidadeLinha(getCart().findIndex(item => item.id === produtoId), action);
}

/** Muda a quantidade de uma linha da sacola (a mesma peça com opções diferentes ocupa linhas diferentes). */
function alterarQuantidadeLinha(index, action) {
    const cart = getCart();
    if (index < 0 || index >= cart.length) return;

    if (action === 'add') {
        cart[index].quantidade += 1;
    } else if (action === 'remove') {
        cart[index].quantidade -= 1;
        if (cart[index].quantidade <= 0) {
            cart.splice(index, 1); // remove item da sacola
        }
    }

    saveCart(cart);
}

function atualizarBadgeCarrinho() {
    const badge = document.getElementById('cart-badge');
    if (!badge) return;
    const cart = getCart();
    const qtdTotal = cart.reduce((acc, item) => acc + item.quantidade, 0);
    badge.textContent = qtdTotal;
}


// ================== LISTA DE PRODUTOS (HOME) ==================
let estadoCategoriaAtual = "Tudo";

function getTermoBusca() {
    const campo = document.getElementById('campo-busca');
    if (!campo) return "";
    return campo.value.trim().toLowerCase();
}

function filtrarProdutos() {
    const termo = getTermoBusca();
    return mockProdutos.filter(p => {
        if (estadoCategoriaAtual !== "Tudo" && p.categoria !== estadoCategoriaAtual) {
            return false;
        }
        if (!termo) return true;
        const alvo = (p.nome + " " + p.descricao).toLowerCase();
        return alvo.includes(termo);
    });
}

function renderizarProdutosNaTela(listaProdutos) {
    const container = document.getElementById('lista-produtos');
    if (!container) return;

    if (!listaProdutos || listaProdutos.length === 0) {
        container.innerHTML = `
            <div class="lista-vazia">
                Nenhum item encontrado.
            </div>
        `;
        return;
    }

    container.innerHTML = "";

    listaProdutos.forEach(prod => {
        const card = `
            <div class="produto-card" data-id="${prod.id}">
                <div class="produto-img">
                    <img src="${prod.imagemUrl}" alt="${prod.nome}" />
                </div>
                <div class="produto-info">
                    <div class="produto-nome">${prod.nome}</div>
                    <div class="produto-desc">${prod.descricao}</div>
                    <div class="produto-preco">R$ ${prod.preco.toFixed(2).replace('.', ',')}</div>
                </div>
                <div class="produto-acoes">
                    <button class="btn-add-carrinho" data-id="${prod.id}">
                        Adicionar
                    </button>
                    <a class="btn-detalhes" href="produto.html?id=${prod.id}">
                        Ver detalhes
                    </a>
                </div>
            </div>
        `;
        container.insertAdjacentHTML('beforeend', card);
    });

    container.querySelectorAll('.btn-add-carrinho').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = parseInt(btn.getAttribute('data-id'), 10);
            addToCartWithFeedback(id);
        });
    });
}

function atualizarListaProdutosFiltrada() {
    const filtrados = filtrarProdutos();
    renderizarProdutosNaTela(filtrados);
}

function inicializarCategoriasELista() {
    const categoriasScroll = document.getElementById('categorias-scroll');
    if (categoriasScroll) {
        categoriasScroll.querySelectorAll('.cat-btn').forEach(botao => {
            botao.addEventListener('click', () => {
                categoriasScroll.querySelectorAll('.cat-btn').forEach(b => b.classList.remove('active'));
                botao.classList.add('active');

                estadoCategoriaAtual = botao.getAttribute('data-cat') || "Tudo";
                atualizarListaProdutosFiltrada();
            });
        });
    }

    atualizarListaProdutosFiltrada();
}

// ================== FINALIZAR PEDIDO / WHATSAPP ==================
function gerarCodigoPedido() {
    const agora = Date.now();
    const sequencia = (agora % 1000000).toString().padStart(6, "0");
    return "LD-" + sequencia; // ex: LD-083412
}

function showWhatsappPrompt(pedido) {
    const overlay     = document.getElementById('modal-whatsapp');
    const titleEl     = document.getElementById('modal-whatsapp-title');
    const textEl      = document.getElementById('modal-whatsapp-text');
    const btnAgoraNao = document.getElementById('modal-whatsapp-cancel-btn');
    const btnWhatsapp = document.getElementById('modal-whatsapp-ok-btn');
    const closeBtn    = document.getElementById('modal-whatsapp-close-btn');

    if (!overlay || !titleEl || !textEl || !btnAgoraNao || !btnWhatsapp) {
        console.warn("Modal whatsapp não encontrado.");
        return;
    }

    const perfil    = getPerfil() || {};
    const itens     = pedido.itens || [];
    const pedidoId  = pedido.id;
    const pagamento = pedido.pagamento;
    const total     = pedido.total || 0;

    titleEl.textContent = "Pedido confirmado";
    textEl.textContent =
        "Seu pedido " + pedidoId +
        " foi confirmado. Você quer acompanhar o status pelo WhatsApp?";

    overlay.style.display = 'flex';

    function encerrarFluxo() {
        clearCart();
        atualizarBadgeCarrinho();

        overlay.style.display = 'none';

        btnAgoraNao.onclick = null;
        btnWhatsapp.onclick = null;
        if (closeBtn) closeBtn.onclick = null;

        if (window.location.pathname.includes('checkout')) {
            window.location.href = 'index.html';
        }
    }

    btnAgoraNao.onclick = encerrarFluxo;
    if (closeBtn) {
        closeBtn.onclick = encerrarFluxo;
    }

    btnWhatsapp.onclick = () => {
        const linhasItens = itens.map(item => {
            const totalExtrasItem = (item.extras || []).reduce(
                (soma, extra) => soma + (extra.preco * extra.qtd),
                0
            );
            const totalItem = valorItem(item);
        
            return `${item.quantidade}x ${item.nome} - R$ ${totalItem.toFixed(2).replace('.', ',')}`;
        });

        const linhaEndereco1 = [perfil.logradouro, perfil.numero].filter(Boolean).join(', ');
        const linhaEndereco2 = [
            perfil.bairro,
            perfil.cidade && perfil.uf ? `${perfil.cidade}/${perfil.uf}` : perfil.cidade || perfil.uf
        ].filter(Boolean).join(' - ');

        const msg = encodeURIComponent(
            `Olá, gostaria de acompanhar meu pedido ${pedidoId}.\n\n` +
            `Pagamento: ${pagamento}\n` +
            `Total: R$ ${total.toFixed(2).replace('.', ',')}\n\n` +
            (perfil.nome ? `Nome: ${perfil.nome}\n` : "") +
            (linhaEndereco1 ? `Endereço: ${linhaEndereco1}\n` : "") +
            (linhaEndereco2 ? `Região: ${linhaEndereco2}\n` : "") +
            (perfil.complemento ? `Complemento: ${perfil.complemento}\n` : "") +
            (perfil.cep ? `CEP: ${perfil.cep}\n` : "") +
            (perfil.telefone ? `Telefone: ${perfil.telefone}\n\n` : "") +
            (linhasItens.length ? `Itens do pedido:\n${linhasItens.join('\n')}` : "")
        );

        const numero = numeroWhatsappDaLoja();
        if (numero) window.open("https://wa.me/" + numero + "?text=" + msg, "_blank");

        encerrarFluxo();
    };
}

function fmtBRL(n) { return Number(n || 0).toLocaleString('pt-BR',{style:'currency',currency:'BRL'}); }

// Monta a descrição no padrão do painel (Produtos / Entrega|Retirada / Cliente / Pagamento / Obs / Total)
function montarDescricaoParaBackend(pedido, perfil) {
  const itens = pedido.itens || [];
  const linhas = [];

  // Produtos
  linhas.push("Produtos:");
  itens.forEach(item => {
    const extrasValor = (item.extras || []).reduce((s,e)=> s + (e.preco * e.qtd), 0);
    const totalItem = valorItem(item);
    // opções agrupadas: "Escolha 4 sabores: 2x Nutella, 2x Red Velvet"
    const porGrupo = {};
    (item.extras || []).filter(e => e.qtd > 0).forEach(e => {
      const g = e.grupo || "Adicionais";
      (porGrupo[g] = porGrupo[g] || []).push(`${e.qtd}x ${e.nome}`);
    });
    const extrasParte = Object.entries(porGrupo).map(([g, itens]) => ` | ${g}: ${itens.join(", ")}`).join("");
    linhas.push(`- ${item.quantidade}x ${item.nome} (${fmtBRL(totalItem)})${extrasParte}`);
  });

  // Encomenda
  if (pedido.agendadoPara) {
    const [dia, hora] = pedido.agendadoPara.split('T');
    linhas.push("", `Encomenda para: ${dia.split('-').reverse().join('/')} às ${hora}`);
  }

  // Entrega / Retirada
  if (pedido.tipoEntrega === 'RETIRADA') {
    linhas.push("", "Retirada no balcão");
  } else {
    const l1 = [perfil?.logradouro, perfil?.numero].filter(Boolean).join(", ");
    const cidadeUf = (perfil?.cidade && perfil?.uf) ? `${perfil.cidade}/${perfil.uf}` : (perfil?.cidade || perfil?.uf || "");
    const l2 = [perfil?.bairro, cidadeUf].filter(Boolean).join(" - ");

    linhas.push("", "Entrega:");
    if (l1) linhas.push(`- ${l1}`);
    if (l2) linhas.push(`- ${l2}`);
    if (perfil?.complemento) linhas.push(`- ${perfil.complemento}`);
  }

  // Cliente
  if (perfil?.nome) {
    linhas.push("", "Cliente:", `- ${perfil.nome}`);
  }

  // Contato
  if (perfil?.telefone) {
    linhas.push("", "Contato:", `- ${perfil.telefone}`);
  }

  // Pagamento
  linhas.push("", "Pagamento:", `- ${pedido.pagamento || "Não informado"}`);

  // Obs
  if (pedido.obs) {
    linhas.push("", "Obs:", `- ${pedido.obs}`);
  }

  // Total
  linhas.push("", "Total:", `- ${fmtBRL(pedido.total)}`);

  return linhas.join("\n");
}

async function enviarPedidoParaBackend(pedido) {
    const perfil = (typeof getPerfil === 'function' ? getPerfil() : {}) || {};
    const descricao = montarDescricaoParaBackend(pedido, perfil);
  
    // ATENÇÃO (Fase 2): o total ainda é calculado aqui no navegador. Na Fase 2 o pedido passa a
    // enviar os itens e o servidor calcula o preço.
    const body = {
      descricao: descricao,
      totalCentavos: Math.round((pedido.total || 0) * 100),
      agendadoPara: pedido.agendadoPara || null
    };

    const resp = await fetch(`${API_LOJA}/pedidos`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Accept": "application/json" },
      body: JSON.stringify(body)
    });

    if (!resp.ok) {
      const erro = await resp.json().catch(() => null);
      const falha = new Error(`Falha ao criar pedido: ${resp.status}`);
      falha.mensagemUsuario = erro && erro.mensagem;
      throw falha;
    }
    return await resp.json(); // PedidoPublicoDto { codigo, status, totalCentavos, criadoEm }
  }

  function gerarCodigoPedido() {
    const segundos = Math.floor(Date.now() / 1000);
    return segundos.toString().slice(-4);
}
  
  async function finalizarPedidoFluxo(metodoPagamento) {
    const perfil = getPerfil && getPerfil();
    if (perfilIncompleto && perfilIncompleto(perfil)) {
      abrirModalPerfilGlobal && abrirModalPerfilGlobal();
      if (typeof registrarEventosCepPerfil === 'function') registrarEventosCepPerfil();
      return;
    }
  
    const cart = getCart && getCart();
    if (!cart || !cart.length) {
      showActionFeedback && showActionFeedback("Sua sacola está vazia", "Adicione itens antes de finalizar.");
      return;
    }
  
    if (!metodoPagamento) {
      showActionFeedback && showActionFeedback("Selecione a forma de pagamento", "Escolha Pix, Cartão ou Dinheiro.");
      return;
    }
  
    // Encomenda: data e horário
    const agendadoPara = agendamentoEscolhido();
    if (VITRINE_LOJA && VITRINE_LOJA.encomenda && VITRINE_LOJA.encomenda.exigeAgendamento && !agendadoPara) {
      showActionFeedback && showActionFeedback("Escolha quando receber", "Selecione o dia e o horário da sua encomenda.");
      return;
    }

    // → tipo de entrega + obs do checkout
    const tipoEntrega = document.querySelector('input[name="tipoEntrega"]:checked')?.value || 'ENTREGA';
    const obs = document.querySelector('#observacoes')?.value?.trim() || "";
  
    // Subtotal
    let subtotal = 0;
    cart.forEach(item => {
      const extrasValor = (item.extras || []).reduce((s,e)=> s + (e.preco * e.qtd), 0);
      subtotal += valorItem(item);
    });
  
    // Taxa de entrega (0 para RETIRADA)
    const taxa = (tipoEntrega === 'RETIRADA') ? 0 : (typeof taxaEntrega !== 'undefined' ? taxaEntrega : 0);
    const total = subtotal + taxa;
    
    const agora = new Date();

    const pedido = {
        id: gerarCodigoPedido(),
        itens: cart,
        subtotal,
        entrega: taxa,
        total,
        pagamento: metodoPagamento,
        tipoEntrega,
        agendadoPara,
        obs,
        status: 'PENDENTE', // pode ser 'pendente' se preferir, o importante é a consistência
        dataIso: agora.toISOString(),
        dataDisplay: agora.toLocaleString('pt-BR'),
        // hora da última mudança de status (começa agora, na criação)
        statusUpdatedAt: agora.toISOString()
      };
  
    // registra no backend
        try {
        const created = await enviarPedidoParaBackend(pedido);
        if (created) {
            // o código é a "chave" para o cliente acompanhar o pedido
            pedido.codigoPublico = created.codigo;
            pedido.code          = created.codigo;
            pedido.id            = created.codigo;

            const novoStatus = created.status || 'NOVO';

            pedido.criadoEm = created.criadoEm;
            pedido.date     = formatarDataPedido(created.criadoEm);  // "Hoje, 15:30"

            // se o status vindo do backend for diferente do que estava,
            // considera que acabou de mudar e atualiza a hora
            if (novoStatus !== pedido.status) {
            pedido.statusUpdatedAt = new Date().toISOString();
            }

            pedido.status = novoStatus;
        }
        } catch (e) {
        console.error(e);
        // mensagens de regra (loja fechada, dia lotado...) vêm prontas do servidor
        showActionFeedback && showActionFeedback("Não foi possível enviar o pedido", e.mensagemUsuario || "Tente novamente em instantes.");
        if (e.mensagemUsuario) inicializarAgendamentoCheckout(); // atualiza dias lotados
        return;
        }
  
    // histórico local do cliente (pedidos.html)
    const pedidos = (getPedidos && getPedidos()) || [];
    pedidos.unshift(pedido);
    savePedidos && savePedidos(pedidos);
  
    // limpa sacola e segue o fluxo normal
    saveCart && saveCart([]);
    showWhatsappPrompt && showWhatsappPrompt(pedido);
  }




// ================== HISTÓRICO DE PEDIDOS ==================
function getPedidos() {
    try {
        const data = localStorage.getItem(CHAVE_PEDIDOS);
        return data ? JSON.parse(data) : [];
    } catch (e) {
        console.error('Erro lendo pedidos:', e);
        return [];
    }
}

function savePedidos(lista) {
    try {
        localStorage.setItem(CHAVE_PEDIDOS, JSON.stringify(lista));
    } catch (e) {
        console.error('Erro salvando pedidos:', e);
    }
}

function isPaginaPedidos() {
    return /pedidos\.html(\?|$)/.test(location.pathname) || document.body.dataset.page === 'pedidos';
  }
  
  // mapeia enum do backend para label do site (se você usa rótulos diferentes)
  function mapStatusLabel(s) {
    switch ((s || "").toUpperCase()) {
      case "NOVO": return "Novo";
      case "VISTO": return "Visto";
      case "EM_PREPARO": return "Em preparo";
      case "SAIU_PARA_ENTREGA": return "Saiu para entrega";
      case "FINALIZADO": return "Finalizado";
      case "CANCELADO": return "Cancelado";
      default: return s || "—";
    }
  }
  
  async function atualizarPedidosComBackendSePossivel() {
    let pedidos = (typeof getPedidos === "function" ? getPedidos() : []) || [];
    if (!pedidos.length) return;
  
    const atualizados = await Promise.all(
      pedidos.map(async (p) => {
        // pedidos sem código público (antigos ou não enviados) → deixa como está
        if (!p.codigoPublico) return p;

        try {
          const r = await fetch(`${API_LOJA}/pedidos/${encodeURIComponent(p.codigoPublico)}`, {
            headers: { "Accept": "application/json" }
          });
  
          if (!r.ok) return p;
  
          const dto = await r.json();
  
          const statusAnterior = p.status;
          const novoStatus = dto.status || statusAnterior;
  
          // por padrão, mantém a hora antiga (se existir)
          let statusUpdatedAt = p.statusUpdatedAt;
  
          // se o backend mudou o status, registra a hora dessa mudança
          if (novoStatus && statusAnterior && novoStatus !== statusAnterior) {
            statusUpdatedAt = new Date().toISOString();
          }
  
          return {
            ...p,
            code: dto.codigo || p.code,
            total: dto.totalCentavos != null ? dto.totalCentavos / 100 : p.total,
  
            // status "cru" vindo do backend (NOVO, VISTO, EM_PREPARO, .)
            status: novoStatus,
  
            // status "bonito" para exibir, se você quiser usar no futuro
            statusLabel: mapStatusLabel(dto.status) || p.statusLabel || p.status,
  
            // hora da última alteração de status
            statusUpdatedAt,
  
            date: dto.criadoEm ? formatarDataPedido(dto.criadoEm) : p.date
          };
        } catch (e) {
          console.error("Erro ao atualizar pedido com backend", e);
          return p;
        }
      })
    );
  
    if (typeof savePedidos === "function") {
      savePedidos(atualizados);
    }
  
    // Página pedidos.html usa preencherListaPedidos()
    if (typeof preencherListaPedidos === "function") {
      preencherListaPedidos();
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    if (typeof isPaginaPedidos === "function" && isPaginaPedidos()) {
      // primeira carga
      atualizarPedidosComBackendSePossivel();
      // atualiza a cada 15s
      setInterval(atualizarPedidosComBackendSePossivel, 15000);
    }
  });  