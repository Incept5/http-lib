package org.incept5.http.interceptors

import org.incept5.http.example.ExampleHttpGateway
import org.incept5.http.example.ExamplePayload
import org.incept5.http.example.SubComponent
import org.incept5.http.interceptors.redaction.RedactConfig
import org.incept5.json.Json
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.slf4j.Logger
import java.util.*

class TestJsonLoggingInterceptor : ShouldSpec({

    context("Test JsonLoggingInterceptor") {

        should("json post request and response are redacted correctly") {

            val server = MockWebServer()

            val logger = mockk<Logger>(relaxed = true)
            val redact = RedactConfig(requestBodyElements = listOf("name", "secret"), responseBodyElements = listOf("name", "secret"))
            val interceptor = JsonLoggingInterceptor(redactConfig = redact)
            interceptor.setLogger(logger)

            val gateway = ExampleHttpGateway(
                baseUri = server.url("/").toString(),
                loggingInterceptor = interceptor
            )

            every { logger.isTraceEnabled } returns true

            val list = mutableListOf<String>()

            every {
                logger.trace(capture(mutableListOf<String>()), capture(list))
            }
            .answers {
                println(it)
                Unit
            }

            val payload = ExamplePayload(UUID.randomUUID(), "foobar", 42, components = arrayOf(SubComponent("password")))

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

            list.forEach {
                it shouldNotContain "password"
                it shouldNotContain "foobar"
                it shouldContain "name\":\"xxxx\""
                it shouldContain "secret\":\"xxxx\""
                it shouldContain payload.id.toString()
            }
        }


        should("get should redact query string correctly") {

            val server = MockWebServer()

            val logger = mockk<Logger>(relaxed = true)
            val redact = RedactConfig(queryParams = listOf("queryname"))
            val interceptor = JsonLoggingInterceptor(redactConfig = redact)
            interceptor.setLogger(logger)

            val gateway = ExampleHttpGateway(
                baseUri = server.url("/").toString(),
                loggingInterceptor = interceptor
            )

            every { logger.isTraceEnabled } returns true

            val list = mutableListOf<String>()

            every {
                logger.trace(capture(mutableListOf<String>()), capture(list))
            }
            .answers {
                println(it)
                Unit
            }

            val payload = ExamplePayload(UUID.randomUUID(), "meow", 43)

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(Json.toJson(payload))
            )

            gateway.getSomethingByQuery("queryname=secret&queryname2=plaintext")

            val request = server.takeRequest()
            request.method shouldBe "GET"
            request.path shouldBe "/something?queryname=secret&queryname2=plaintext"

            server.shutdown()

            val rq = list[0]
            rq shouldNotContain "secret"
            rq shouldContain "plaintext"
        }

        should("xml post request and response are redacted correctly") {

            val server = MockWebServer()

            val logger = mockk<Logger>(relaxed = true)
            val redact = RedactConfig(requestBodyElements = listOf("name", "secret"), responseBodyElements = listOf("name", "secret"))
            val interceptor = JsonLoggingInterceptor(redactConfig = redact)
            interceptor.setLogger(logger)

            val gateway = ExampleHttpGateway(
                baseUri = server.url("/").toString(),
                loggingInterceptor = interceptor
            )

            every { logger.isTraceEnabled } returns true

            val list = mutableListOf<String>()

            every {
                logger.trace(capture(mutableListOf<String>()), capture(list))
            }
            .answers {
                println(it)
                Unit
            }
            val id = UUID.randomUUID()

            val payload = "<ExamplePayload><id>${id}</id><name>foobar</name><age>42</age><components><secret>password</secret></components></ExamplePayload>"

            server.enqueue(
                MockResponse()
                    .setResponseCode(201)
                    .setHeader("Content-Type", "application/xml")
                    .setBody(payload)
            )

            gateway.postSomethingAsXml(payload)

            val request = server.takeRequest()
            request.method shouldBe "POST"
            request.path shouldBe "/somethingXml"
            request.body.readUtf8() shouldBe payload

            server.shutdown()

            list.forEach {
                it shouldNotContain "password"
                it shouldNotContain "foobar"
                it shouldContain "name>xxxx<"
                it shouldContain "secret>xxxx<"
                it shouldContain id.toString()
            }
        }
    }

    should("logging without redaction works correctly") {

        val server = MockWebServer()

        val logger = mockk<Logger>(relaxed = true)
        val loggerWithoutRedaction = mockk<Logger>(relaxed = true)
        val redact = RedactConfig(requestBodyElements = listOf("name", "secret"), responseBodyElements = listOf("name", "secret"))
        val interceptor = JsonLoggingInterceptor(redactConfig = redact)
        interceptor.setLogger(logger, loggerWithoutRedaction)

        val gateway = ExampleHttpGateway(
            baseUri = server.url("/").toString(),
            loggingInterceptor = interceptor
        )

        every { logger.isTraceEnabled } returns true
        every { loggerWithoutRedaction.isTraceEnabled } returns true

        val list = mutableListOf<String>()

        every {
            loggerWithoutRedaction.trace(capture(mutableListOf<String>()), capture(list))
        }
            .answers {
                println(it)
                Unit
            }
        val id = UUID.randomUUID()

        val payload = "<ExamplePayload><id>${id}</id><name>foobar</name><age>42</age><components><secret>password</secret></components></ExamplePayload>"

        server.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/xml")
                .setBody(payload)
        )

        gateway.postSomethingAsXml(payload)

        val request = server.takeRequest()
        request.method shouldBe "POST"
        request.path shouldBe "/somethingXml"
        request.body.readUtf8() shouldBe payload

        server.shutdown()

        list.forEach {
            it shouldContain "name>foobar<"
            it shouldContain "secret>password<"
            it shouldContain id.toString()
        }
    }

})