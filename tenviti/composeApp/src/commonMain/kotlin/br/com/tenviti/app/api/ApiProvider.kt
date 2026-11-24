package br.com.tenviti.app.api

import io.ktor.client.HttpClient

object ApiProvider {
    val httpClient: HttpClient by lazy { createHttpClient() }
    val uploadClient: HttpClient by lazy { createUploadClient() }
    val painelApi: PainelApi by lazy { PainelApi(httpClient, uploadClient) }
}
