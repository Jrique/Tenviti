package br.com.tenviti.app

import androidx.compose.runtime.Composable

@Composable
actual fun rememberWindowSize(): WindowSize {
    return WindowSize.COMPACT
}