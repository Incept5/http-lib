package org.incept5.http.auth

/**
 * Fetch a new auth token
 */
interface AuthTokenFetcher {

    /**
     * Fetch a new auth token
     */
    fun fetchNewToken(): TokenResponse

    fun refreshToken(lastResponse: TokenResponse?): TokenResponse {
        // default implementation just fetches a new token
        return fetchNewToken()
    }
}