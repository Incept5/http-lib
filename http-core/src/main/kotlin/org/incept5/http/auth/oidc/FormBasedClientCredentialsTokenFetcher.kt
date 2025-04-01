package org.incept5.http.auth.oidc

import org.incept5.http.auth.AuthTokenFetcher
import org.incept5.http.auth.TokenResponse
import org.incept5.http.interceptors.RetryInterceptor
import org.incept5.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches a new token using the client credentials grant,
 * with the client credentials provided in a form body
 * and assumes the normal OIDC style token endpoint
 */
class FormBasedClientCredentialsTokenFetcher(val config: ClientCredentialsConfig) : AuthTokenFetcher {

    private val client = OkHttpClient.Builder().addInterceptor(RetryInterceptor()).build()

    override fun fetchNewToken(): TokenResponse {
        val formBody = FormBody.Builder()
            .add("grant_type", "client_credentials")
            .add("client_id", config.clientId())
            .add("client_secret", config.clientSecret())
            .build()
        val request = Request.Builder()
            .url(config.tokenEndpoint())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(formBody)
            .build()
        client.newCall(request).execute().use { response ->
            val bodyAsString = response.body?.string()
            return Json.fromJson(bodyAsString!!)
        }
    }
}