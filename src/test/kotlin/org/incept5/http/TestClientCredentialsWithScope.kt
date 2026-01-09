
package org.incept5.http

import org.incept5.http.auth.TokenResponse
import org.incept5.http.auth.oidc.ClientCredentialsTokenFetcher
import org.incept5.http.auth.oidc.FormBasedClientCredentialsTokenFetcher
import org.incept5.http.auth.oidc.StdClientCredentialsConfig
import org.incept5.json.Json
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class TestClientCredentialsWithScope : ShouldSpec({

    context("ClientCredentialsTokenFetcher with scope") {

        should("include scope in token request when provided") {
            val server = MockWebServer()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(TokenResponse(access_token = "access_token_with_scope")))
            )

            val baseUrl = server.url("/").toString()

            val authConfig = StdClientCredentialsConfig(
                tokenEndpoint = "${baseUrl}token",
                clientSecret = "client_secret",
                clientId = "client_id",
                scope = "payment:create payment:read webhook:read webhook:write"
            )

            val tokenFetcher = ClientCredentialsTokenFetcher(authConfig)
            val token = tokenFetcher.fetchNewToken()

            token.access_token shouldBe "access_token_with_scope"

            val request = server.takeRequest()
            request.method shouldBe "POST"
            request.path shouldBe "/token"
            
            val body = request.body.readUtf8()
            body shouldContain "grant_type=client_credentials"
            body shouldContain "scope=payment:create payment:read webhook:read webhook:write"

            server.shutdown()
        }

        should("work without scope when not provided") {
            val server = MockWebServer()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(TokenResponse(access_token = "access_token_no_scope")))
            )

            val baseUrl = server.url("/").toString()

            val authConfig = StdClientCredentialsConfig(
                tokenEndpoint = "${baseUrl}token",
                clientSecret = "client_secret",
                clientId = "client_id"
            )

            val tokenFetcher = ClientCredentialsTokenFetcher(authConfig)
            val token = tokenFetcher.fetchNewToken()

            token.access_token shouldBe "access_token_no_scope"

            val request = server.takeRequest()
            request.method shouldBe "POST"
            request.path shouldBe "/token"
            
            val body = request.body.readUtf8()
            body shouldBe "grant_type=client_credentials"

            server.shutdown()
        }

    }

    context("FormBasedClientCredentialsTokenFetcher with scope") {

        should("include scope in form body when provided") {
            val server = MockWebServer()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(TokenResponse(access_token = "access_token_with_scope")))
            )

            val baseUrl = server.url("/").toString()

            val authConfig = StdClientCredentialsConfig(
                tokenEndpoint = "${baseUrl}token",
                clientSecret = "client_secret",
                clientId = "client_id",
                scope = "payment:create payment:read webhook:read webhook:write"
            )

            val tokenFetcher = FormBasedClientCredentialsTokenFetcher(authConfig)
            val token = tokenFetcher.fetchNewToken()

            token.access_token shouldBe "access_token_with_scope"

            val request = server.takeRequest()
            request.method shouldBe "POST"
            request.path shouldBe "/token"
            request.getHeader("Content-Type") shouldBe "application/x-www-form-urlencoded"
            
            val body = request.body.readUtf8()
            body shouldContain "grant_type=client_credentials"
            body shouldContain "client_id=client_id"
            body shouldContain "client_secret=client_secret"
            body shouldContain "scope="
            // Verify scope is present with URL encoding (spaces as + and colons as %3A)
            (body.contains("payment") && body.contains("webhook")) shouldBe true

            server.shutdown()
        }

        should("work without scope when not provided") {
            val server = MockWebServer()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(TokenResponse(access_token = "access_token_no_scope")))
            )

            val baseUrl = server.url("/").toString()

            val authConfig = StdClientCredentialsConfig(
                tokenEndpoint = "${baseUrl}token",
                clientSecret = "client_secret",
                clientId = "client_id"
            )

            val tokenFetcher = FormBasedClientCredentialsTokenFetcher(authConfig)
            val token = tokenFetcher.fetchNewToken()

            token.access_token shouldBe "access_token_no_scope"

            val request = server.takeRequest()
            request.method shouldBe "POST"
            request.path shouldBe "/token"
            
            val body = request.body.readUtf8()
            body shouldContain "grant_type=client_credentials"
            body shouldContain "client_id=client_id"
            body shouldContain "client_secret=client_secret"
            body.contains("scope") shouldBe false

            server.shutdown()
        }

    }

})
