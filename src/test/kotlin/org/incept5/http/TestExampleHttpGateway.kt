package org.incept5.http

import org.incept5.http.error.HttpException
import org.incept5.http.example.ExampleHttpGateway
import org.incept5.http.example.ExamplePayload
import org.incept5.http.example.SubComponent
import org.incept5.json.Json
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.UUID

class TestExampleHttpGateway : ShouldSpec({

    context("Test ExampleHttpGateway") {


        should("get something by id") {

            val server = MockWebServer()
            val gateway = ExampleHttpGateway(server.url("/").toString())

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

            server.shutdown()
        }

        should("get something by id - with path in baseUri") {

            val server = MockWebServer()
            val gateway = ExampleHttpGateway(server.url("/path").toString())

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
            request.path shouldBe "/path/something/$id"

            server.shutdown()
        }

        should("get something by query") {

            val server = MockWebServer()
            val gateway = ExampleHttpGateway(server.url("/").toString())

            val query = "name=John&age=42"

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(Json.toJson(ExamplePayload(UUID.randomUUID(), "get_by_query", 42)))
            )

            val result = gateway.getSomethingByQuery(query)

            result.name shouldBe "get_by_query"

            val request = server.takeRequest()
            request.method shouldBe "GET"
            request.path shouldBe "/something?$query"

            server.shutdown()
        }

        should("post something") {

            val server = MockWebServer()
            val gateway = ExampleHttpGateway(server.url("/").toString())

            val payload = ExamplePayload(UUID.randomUUID(), "post", 42, components = arrayOf(SubComponent("secret")))

            server.enqueue(
                MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/json")
                    .setBody(Json.toJson(payload))
            )

            gateway.postSomething(payload)

            val request = server.takeRequest()
            request.method shouldBe "POST"
            request.path shouldBe "/something"
            request.body.readUtf8() shouldBe Json.toJson(payload)

            server.shutdown()
        }

        should("put something") {

            val server = MockWebServer()
            val gateway = ExampleHttpGateway(server.url("/").toString())

            val id = UUID.randomUUID()
            val payload = ExamplePayload(id, "put", 42, components = arrayOf(SubComponent("secret")))

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(Json.toJson(payload))
            )

            gateway.putSomething(id, payload)

            val request = server.takeRequest()
            request.method shouldBe "PUT"
            request.path shouldBe "/something/$id"
            request.body.readUtf8() shouldBe Json.toJson(payload)

            server.shutdown()
        }

        should("delete something") {

            val server = MockWebServer()
            val gateway = ExampleHttpGateway(server.url("/").toString())

            val id = UUID.randomUUID()

            server.enqueue(
                MockResponse()
                    .setResponseCode(204)
            )

            gateway.deleteSomething(id)

            val request = server.takeRequest()
            request.method shouldBe "DELETE"
            request.path shouldBe "/something/$id"

            server.shutdown()
        }

        should("patch something") {

            val server = MockWebServer()
            val gateway = ExampleHttpGateway(server.url("/").toString())

            val id = UUID.randomUUID()
            val payload = ExamplePayload(id, "patch", 42)

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(Json.toJson(payload))
            )

            gateway.patchSomething(id, payload)

            val request = server.takeRequest()
            request.method shouldBe "PATCH"
            request.path shouldBe "/something/$id"
            request.body.readUtf8() shouldBe Json.toJson(payload)

            server.shutdown()
        }

        should("500 causes retryable") {

                val server = MockWebServer()
                val gateway = ExampleHttpGateway(server.url("/").toString())

                val id = UUID.randomUUID()

                // need 5 of them due to the retry interceptor
                server.enqueue(
                    MockResponse()
                        .setResponseCode(500)
                )
                server.enqueue(
                    MockResponse()
                        .setResponseCode(500)
                )
                server.enqueue(
                    MockResponse()
                        .setResponseCode(500)
                )
                server.enqueue(
                    MockResponse()
                        .setResponseCode(500)
                )
                server.enqueue(
                    MockResponse()
                        .setResponseCode(500)
                )

                val result = runCatching {
                    gateway.getSomethingById(id)
                }

                result.isFailure shouldBe true
                val exp = result.exceptionOrNull()!! as HttpException
                exp.message shouldBe "http request failed with code: 500"
                exp.retryable shouldBe true

                server.shutdown()
        }

        should("409 causes retryable") {

            val server = MockWebServer()
            val gateway = ExampleHttpGateway(server.url("/").toString())

            val id = UUID.randomUUID()

            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
            )

            server.enqueue(
                MockResponse()
                    .setResponseCode(409)
            )

            val result = runCatching {
                gateway.getSomethingById(id)
            }

            result.isFailure shouldBe true
            val exp = result.exceptionOrNull()!! as HttpException
            exp.message shouldBe "http request failed with code: 409"
            exp.retryable shouldBe true

            server.shutdown()
        }
    }

})