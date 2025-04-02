package org.incept5.http

import org.incept5.http.example.ExampleGatewayWithBasicAuth
import org.incept5.http.example.ExamplePayload
import org.incept5.json.Json
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.UUID

class TestBasicAuthnInterceptor : ShouldSpec({

    context("Test ExampleGatewayWithBasicAuth") {


        should("get something by id") {

            val server = MockWebServer()
            val gateway = ExampleGatewayWithBasicAuth(
                server.url("/").toString(), "user", "password"
            )

            val id = UUID.randomUUID()

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(ExamplePayload(id, "get_by_id", 42)))
            )

            val result = gateway.getSomethingById(id)

            result.id shouldBe id

            val request = server.takeRequest()
            request.method shouldBe "GET"
            request.path shouldBe "/something/$id"

            val authHeader = request.getHeader("Authorization")
            val expectedAuthHeader = "Basic dXNlcjpwYXNzd29yZA==" // Base64 encoded "user:password"
            assert(authHeader == expectedAuthHeader)

            server.shutdown()
        }
    }

})