package org.incept5.http

import org.incept5.http.auth.TokenResponse
import org.incept5.http.auth.oidc.ClientCredentialsTokenFetcher
import org.incept5.http.auth.oidc.StdClientCredentialsConfig
import org.incept5.http.example.ExampleGatewayWithAuth
import org.incept5.http.example.ExamplePayload
import org.incept5.json.Json
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.*

class TestHttpAuthInterceptor : ShouldSpec({

    context("Test Auth Interceptor") {

        should("interceptor fetches initial token") {
            val server = MockWebServer()

            val id = UUID.randomUUID()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(TokenResponse(access_token = "access_token")))
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(ExamplePayload(id, "get_by_id", 42)))
            )

            val baseUrl = server.url("/").toString()

            val authConfig = StdClientCredentialsConfig(
                tokenEndpoint = "${baseUrl}token",
                clientSecret = "client_secret",
                clientId = "client_id"
            )

            val tokenFetcher = ClientCredentialsTokenFetcher(authConfig)
            val gateway = ExampleGatewayWithAuth(baseUrl, tokenFetcher)

            val result = gateway.getSomethingById(id)

            result.id shouldBe id

            val authRequest = server.takeRequest()
            authRequest.method shouldBe "POST"
            authRequest.path shouldBe "/token"

            val request = server.takeRequest()
            request.method shouldBe "GET"
            request.path shouldBe "/something/$id"

            server.shutdown()
        }

        should("401 causes token refresh") {
            val server = MockWebServer()

            val id = UUID.randomUUID()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(TokenResponse(access_token = "access_token")))
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(TokenResponse(access_token = "access_token")))
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(ExamplePayload(id, "get_by_id", 42)))
            )

            val baseUrl = server.url("/").toString()

            val authConfig = StdClientCredentialsConfig(
                tokenEndpoint = "${baseUrl}token",
                clientSecret = "client_secret",
                clientId = "client_id"
            )

            val tokenFetcher = ClientCredentialsTokenFetcher(authConfig)
            val gateway = ExampleGatewayWithAuth(baseUrl, tokenFetcher)

            val result = gateway.getSomethingById(id)

            result.id shouldBe id

            val authRequest1 = server.takeRequest()
            authRequest1.method shouldBe "POST"
            authRequest1.path shouldBe "/token"

            val request2 = server.takeRequest()
            request2.method shouldBe "GET"
            request2.path shouldBe "/something/$id"

            val authRequest3 = server.takeRequest()
            authRequest3.method shouldBe "POST"
            authRequest3.path shouldBe "/token"

            val request4 = server.takeRequest()
            request4.method shouldBe "GET"
            request4.path shouldBe "/something/$id"

            server.shutdown()
        }

    }

})