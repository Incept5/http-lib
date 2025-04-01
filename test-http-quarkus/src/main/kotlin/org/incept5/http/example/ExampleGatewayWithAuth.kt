package org.incept5.http.example

import org.incept5.http.auth.AuthTokenFetcher
import org.incept5.http.client.HttpClient
import java.util.*

/**
 * An Example Http Gateway
 */
class ExampleGatewayWithAuth (baseUri: String, tokenFetcher: AuthTokenFetcher) : HttpClient(baseUri, tokenFetcher = tokenFetcher) {

    /**
     * Get something by id and marshal the response into an ExamplePayload
     */
    fun getSomethingById (id: UUID) : ExamplePayload {
        return super.get("/something/$id", null)
    }

}