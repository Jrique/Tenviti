package br.com.tenviti.app.data

import androidx.compose.ui.graphics.Color

data class Pedido(
    val id: Long,
    val codigo: String,
    val descricao: String,
    val valor: String,
    val horario: String,
    val status: String,
    val corStatus: Color
)