package br.com.tenviti.app

import androidx.compose.runtime.Composable


enum class WindowSize {
    COMPACT, // Celulares
    MEDIUM,  // Tablets
    EXPANDED // Desktop e Web em tela cheia
}

@Composable
expect fun rememberWindowSize(): WindowSize