package org.incept5.http.example

import org.incept5.http.auth.oidc.ClientCredentialsConfig
import io.smallrye.config.ConfigMapping
import java.util.Optional

@ConfigMapping(prefix = "example.http")
interface ExampleHttpConfig {

    fun baseUrl(): String
    
    fun tokenEndpoint(): String
    
    fun clientId(): String
    
    fun clientSecret(): String
    
    fun scopeOptional(): Optional<String>

}

/**
 * Adapter to bridge ExampleHttpConfig to ClientCredentialsConfig
 */
class ExampleHttpConfigAdapter(private val config: ExampleHttpConfig) : ClientCredentialsConfig {
    override fun tokenEndpoint(): String = config.tokenEndpoint()
    override fun clientId(): String = config.clientId()
    override fun clientSecret(): String = config.clientSecret()
    override fun scope(): String? = config.scopeOptional().orElse(null)
}