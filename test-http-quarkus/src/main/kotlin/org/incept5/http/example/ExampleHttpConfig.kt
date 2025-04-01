package org.incept5.http.example

import org.incept5.http.auth.oidc.ClientCredentialsConfig
import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "example.http")
interface ExampleHttpConfig : ClientCredentialsConfig {

    fun baseUrl(): String

}