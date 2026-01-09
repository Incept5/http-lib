package org.incept5.http.auth.oidc

/**
 * Client Credentials configuration for OIDC
 */
interface ClientCredentialsConfig {

    fun tokenEndpoint(): String

    fun clientId(): String

    fun clientSecret(): String

    /**
     * Optional space-separated list of scopes to request
     * e.g. "payment:create payment:read webhook:read"
     * @return space-separated scope string, or null if no scopes requested
     */
    fun scope(): String?

}