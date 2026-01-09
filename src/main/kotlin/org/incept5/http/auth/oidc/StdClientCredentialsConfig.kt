package org.incept5.http.auth.oidc

data class StdClientCredentialsConfig (
    val tokenEndpoint: String,
    val clientId: String,
    val clientSecret: String,
    val scope: String? = null
) : ClientCredentialsConfig {
    override fun tokenEndpoint(): String {
        return tokenEndpoint
    }

    override fun clientId(): String {
        return clientId
    }

    override fun clientSecret(): String {
        return clientSecret
    }

    override fun scope(): String? {
        return scope
    }
}