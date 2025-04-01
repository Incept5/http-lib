package org.incept5.http.example

import org.incept5.http.auth.BasicAuthenticationInterceptor
import org.incept5.http.client.HttpClient
import okhttp3.Interceptor
import java.util.*

/**
 * An Example Http Gateway
 */
open class ExampleGatewayWithBasicAuth (baseUri: String, username: String, password: String)
    : HttpClient(baseUri = baseUri, interceptors = listOf(BasicAuthenticationInterceptor(username, password))) {

    /**
     * Get something by id and marshal the response into an ExamplePayload
     */
    fun getSomethingById (id: UUID) : ExamplePayload {
        return super.get("/something/$id", null)
    }

}