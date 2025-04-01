package org.incept5.http.auth.oidc

/**
 * Client Credentials configuration for OIDC
 */
interface ClientCredentialsConfig {

    fun tokenEndpoint(): String

    fun clientId(): String

    fun clientSecret(): String

}