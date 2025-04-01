package org.incept5.http.auth.oidc

import org.incept5.http.auth.AuthTokenFetcher
import org.incept5.http.auth.TokenResponse
import org.incept5.http.interceptors.RetryInterceptor
import org.incept5.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

/**
 * Fetches a new token using the client credentials grant
 * and assumes the normal OIDC style token endpoint
 */
class ClientCredentialsTokenFetcher (val config: ClientCredentialsConfig): AuthTokenFetcher {

    private val encoder = Base64.getEncoder()
    private val client = OkHttpClient.Builder().addInterceptor(RetryInterceptor()).build()

    override fun fetchNewToken(): TokenResponse {
        val token = encoder.encode("${config.clientId()}:${config.clientSecret()}".toByteArray()).toString(Charsets.UTF_8)
        val body = "grant_type=client_credentials".toRequestBody(null)
        val request = Request.Builder()
            .url(config.tokenEndpoint())
            .header("Authorization", "Basic $token")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val bodyAsString = response.body?.string()
            return Json.fromJson(bodyAsString!!)
        }
    }
}