package org.incept5.http.example

import org.incept5.http.client.HttpClient
import org.incept5.http.interceptors.JsonLoggingInterceptor
import org.incept5.http.interceptors.redaction.RedactConfig
import org.incept5.json.Json
import jakarta.xml.bind.JAXB
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.StringWriter
import java.util.*

/**
 * An Example Http Gateway
 */
open class ExampleHttpGateway (
        baseUri: String,
        loggingInterceptor: JsonLoggingInterceptor = JsonLoggingInterceptor(
            RedactConfig(
                requestBodyElements = listOf("name", "secret"),
                responseBodyElements = listOf("name", "secret")
            )
        )
    ) : HttpClient(
        baseUri = baseUri,
        loggingInterceptor = loggingInterceptor
) {

    /**
     * Get something by id and marshal the response into an ExamplePayload
     */
    fun getSomethingById (id: UUID) : ExamplePayload {
        return super.get("/something/$id", null)
    }

    fun getSomethingByQuery (query: String) : ExamplePayload {
        return super.get("/something", query)
    }

    fun postSomething (payload: ExamplePayload) {
        super.postJson<ExamplePayload>("/something", payload)
    }

    fun postSomethingAsXml (payload: String) : String {
        val url = url("/somethingXml")
        val request = Request.Builder()
            .url(url)
            .post(requestBody(payload, "application/xml"))
            .build()
        return execute(request) { response ->
            response.body?.string()!!
        }
    }

    fun putSomething (id: UUID, payload: ExamplePayload) {
        super.putJson<ExamplePayload>("/something/$id", payload)
    }

    fun deleteSomething (id: UUID) {
        super.delete("/something/$id")
    }

    fun patchSomething (id: UUID, payload: ExamplePayload) {
        super.patchJson<ExamplePayload>("/something/$id", payload)
    }
}