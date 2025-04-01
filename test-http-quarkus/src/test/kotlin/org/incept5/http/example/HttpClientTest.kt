package org.incept5.http.example

import com.github.tomakehurst.wiremock.client.WireMock
import org.incept5.http.client.HttpClient
import org.incept5.http.interceptors.RetryInterceptor
import io.quarkiverse.wiremock.devservice.ConnectWireMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@QuarkusTest
class HttpClientTest {

    @ConfigProperty(name = "test.url")
    lateinit var testUrl: String

    /**
     * This test will use 10 threads to call the same endpoint which sometimes returns a 409 conflict
     */
    @Test
    fun `should handle retries under load`(){


        val client = HttpClient(baseUri = testUrl, retryInterceptor = RetryInterceptor(maxRetries = 10, pauseBetweenRetriesMs = 100))

        val callable: () -> String = {
            client.getString("/example")
        }

        val executor = Executors.newFixedThreadPool(10)

        val futures = (1..100).map {
            executor.submit(callable)
        }

        futures.forEach {
            val response: String? = it.get(5, TimeUnit.SECONDS) as String
            assert(response != null)
            assert(response!! == "GOOD_RESPONSE")
        }
    }
}