package br.com.tenviti.app.api

import kotlinx.browser.window

// Usa o mesmo host em que o painel web está aberto, na porta do servidor Ktor.
actual fun apiBaseUrl(): String = "http://${window.location.hostname}:8081"
