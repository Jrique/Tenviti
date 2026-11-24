package br.com.tenviti.app

import androidx.compose.runtime.*
import kotlinx.browser.window
import org.w3c.dom.events.Event

@Composable
actual fun rememberWindowSize(): WindowSize {
    var windowSize by remember { mutableStateOf(getWindowSize()) }

    DisposableEffect(Unit) {
        val onResize: (Event) -> Unit = {
            windowSize = getWindowSize()
        }
        window.addEventListener("resize", onResize)
        onDispose {
            window.removeEventListener("resize", onResize)
        }
    }
    return windowSize
}

private fun getWindowSize(): WindowSize {
    val width = window.innerWidth
    return when {
        width < 600 -> WindowSize.COMPACT
        width < 840 -> WindowSize.MEDIUM
        else -> WindowSize.EXPANDED
    }
}