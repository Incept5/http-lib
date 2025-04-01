package org.incept5.http.auth

import okhttp3.Interceptor
import okhttp3.Response
import java.util.Base64

/**
 * Interceptor that adds Basic Authentication header to the request
 * using the provided username and password
 */
class BasicAuthenticationInterceptor(
    private val username: String,
    private val password: String,
    private val authHeader: String = "Authorization") : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authenticatedRequest = addAuthHeader(request)
        return chain.proceed(authenticatedRequest)
    }

    private fun addAuthHeader(request: okhttp3.Request): okhttp3.Request {
        val credentials = "$username:$password"
        val basicToken = Base64.getEncoder().encodeToString(credentials.toByteArray())
        val authHeaderValue = "Basic $basicToken"
        return request.newBuilder()
            .addHeader(authHeader, authHeaderValue)
            .build()
    }
}
