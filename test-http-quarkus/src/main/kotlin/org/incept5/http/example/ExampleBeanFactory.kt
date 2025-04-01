package org.incept5.http.example

import org.incept5.http.auth.oidc.ClientCredentialsTokenFetcher
import io.quarkus.runtime.Startup
import jakarta.inject.Singleton

/**
 * Example of how to create a gateway with config and auth
 */
class ExampleBeanFactory {

    @Singleton
    @Startup
    fun createExampleGatewayWithAuth(config: ExampleHttpConfig): ExampleGatewayWithAuth {
        val tokenFetcher = ClientCredentialsTokenFetcher(config)
        return ExampleGatewayWithAuth(config.baseUrl(), tokenFetcher)
    }

}