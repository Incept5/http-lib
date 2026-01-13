package org.incept5.http.auth.oidc

import org.incept5.http.auth.AuthTokenFetcher
import org.incept5.http.auth.TokenResponse
import org.incept5.http.interceptors.RetryInterceptor
import org.incept5.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Fetches a new token using the client credentials grant
 * with JSON request body instead of form parameters
 */
class ClientCredentialsTokenFetcher (val config: ClientCredentialsConfig): AuthTokenFetcher {

    private val client = OkHttpClient.Builder().addInterceptor(RetryInterceptor()).build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun fetchNewToken(): TokenResponse {
        val requestPayload = buildMap {
            put("grant_type", "client_credentials")
            put("client_id", config.clientId())
            put("client_secret", config.clientSecret())
            config.scope()?.let { scope ->
                put("scope", scope)
            }
        }
        
        val bodyJson = Json.toJson(requestPayload)
        val body = bodyJson.toRequestBody(jsonMediaType)
        
        val request = Request.Builder()
            .url(config.tokenEndpoint())
            .post(body)
            .build()
            
        client.newCall(request).execute().use { response ->
            val bodyAsString = response.body?.string()
            return Json.fromJson(bodyAsString!!)
        }
    }
}