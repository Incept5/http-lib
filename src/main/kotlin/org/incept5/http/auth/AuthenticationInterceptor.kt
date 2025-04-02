package org.incept5.http.auth

import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Abstract interceptor that handles the authentication for the requests
 * You just need to implement AuthTokenFetcher and this class will use it
 * to fetch new tokens when needed
 */
open class AuthenticationInterceptor(private val tokenFetcher: AuthTokenFetcher, private val authHeader: String = "Authorization") : Interceptor {

    private var lastTokenResponse : TokenResponse? = null
    private var currentTokenExpiresAt : Instant? = null

    companion object {
        private val logger = LoggerFactory.getLogger(AuthenticationInterceptor::class.java)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if ( request.header(authHeader) == null ) {
            val token = getToken()

            val response = chain.proceed(addAuthHeader(request, token))
            // if we get a 401, try to refresh the token and retry the request
            if ( response.code == 401 ) {
                logger.info ( "Received 401, refreshing token and retrying request" )
                refreshToken()
                val newToken = getToken()
                return chain.proceed(addAuthHeader(request, newToken))
            }
            return response
        }
        return chain.proceed(request)
    }

    protected fun getToken(): TokenResponse {
        if ( lastTokenResponse == null ){
            refreshToken()
        }
        else if ( currentTokenExpiresAt != null && currentTokenExpiresAt!!.isBefore(Instant.now()) ){
            refreshToken()
        }
        return lastTokenResponse!!
    }

    protected fun refreshToken() {
        logger.trace ( "Refreshing auth token" )
        val newTokenResponse = tokenFetcher.refreshToken(lastTokenResponse)
        if ( newTokenResponse.expires_in != null ) {
            currentTokenExpiresAt = Instant.now().plusSeconds(newTokenResponse.expires_in.toLong() - 10)
            logger.debug ( "New Auth Token expires at: {}", currentTokenExpiresAt )
        }
        lastTokenResponse = newTokenResponse
    }

    /**
     * This will add a standard Bearer token header to the request
     */
    protected fun addAuthHeader(request: okhttp3.Request, token: TokenResponse): okhttp3.Request {
        return request.newBuilder()
            .addHeader(authHeader, "Bearer ${token.access_token}")
            .build()
    }
}