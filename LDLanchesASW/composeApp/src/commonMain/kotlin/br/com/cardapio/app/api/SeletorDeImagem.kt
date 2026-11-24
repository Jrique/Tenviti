package br.com.cardapio.app.api

import androidx.compose.runtime.Composable
import br.com.cardapio.app.data.ArquivoSelecionado

/**
 * Abre o seletor de imagem da plataforma (galeria no Android, janela de arquivos no desktop,
 * input de arquivo no navegador). Devolve a função que abre o seletor.
 */
@Composable
expect fun rememberSeletorDeImagem(aoEscolher: (ArquivoSelecionado) -> Unit): () -> Unit
