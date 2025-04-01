package org.incept5.http.client

import org.incept5.http.auth.AuthTokenFetcher
import org.incept5.http.auth.AuthenticationInterceptor
import org.incept5.http.interceptors.CorrelationIdInterceptor
import org.incept5.http.interceptors.JsonLoggingInterceptor
import org.incept5.http.interceptors.RetryInterceptor
import org.incept5.json.Json
import org.incept5.telemetry.log.LogEvent
import org.incept5.telemetry.log.VSLogger
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URL
import java.util.function.Function

/**
 * Base class for our Incept5 Http Gateways to be used when invoking external services
 * or making calls to other Incept5 micro-services
 *
 * See readme for usage information
 *
 */
open class HttpClient (
    protected val baseUri: String,
    val failureHandler: HttpFailureHandler = DefaultHttpFailureHandler(),
    protected val tokenFetcher: AuthTokenFetcher? = null,
    protected val retryInterceptor: Interceptor? = RetryInterceptor(),
    protected val loggingInterceptor: Interceptor? = JsonLoggingInterceptor(), // Logbook style logging as default
    protected val interceptors: List<Interceptor> = emptyList(),
    clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
) {

    companion object {
        val MEDIA_TYPE_JSON = "application/json"
        private val log = VSLogger(HttpClient::class.java)
    }

    // make the underlying client available in case it's needed
    var okHttpClient: OkHttpClient = buildClient(clientBuilder)

    private fun buildClient(clientBuilder: OkHttpClient.Builder): OkHttpClient {
        log.trace ( "Building http client : {}", LogEvent("baseUri" to baseUri) )
        addStandardInterceptors(clientBuilder)
        return clientBuilder.build()
    }

    protected open fun addStandardInterceptors(clientBuilder: OkHttpClient.Builder) {

        // add any custom interceptors
        if ( interceptors.isNotEmpty() ){
            interceptors.forEach {
                log.trace( "Adding custom interceptor : {}", LogEvent("cls" to it.javaClass.name ))
                clientBuilder.addInterceptor(it)
            }
        }

        // add correlation id interceptor
        clientBuilder.addInterceptor(CorrelationIdInterceptor())

        // add standard retry interceptor
        if ( retryInterceptor != null ){
            clientBuilder.addInterceptor(retryInterceptor)
        }

        if ( tokenFetcher != null ){
            // add auth interceptor
            clientBuilder.addInterceptor(AuthenticationInterceptor(tokenFetcher))
        }

        // add logging interceptor
        if ( loggingInterceptor != null ){
            clientBuilder.addInterceptor(loggingInterceptor)
        }
    }

    /**
     * Create a URL from the baseUri and the path
     * and optionally a query string
     */
    fun url(path: String, query: String? = null): URL {
        val resolvedPath = if (baseUri.endsWith("/")) {
            "$baseUri${path.removePrefix("/")}"
        } else {
            "$baseUri$path"
        }
        return if (query == null) {
            URI.create(resolvedPath).toURL()
        } else {
            URI.create("$resolvedPath?$query").toURL()
        }
    }

    protected fun queryString(map: Map<String, String>): String {
        return map.map { "${it.key}=${it.value}" }.joinToString("&")
    }

    /**
     * Execute any request and handle the response using the configured client
     */
    inline fun <reified T> execute (request: Request, handler: Function<Response,T>): T {
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                // hand off to failure handler to throw exceptions if needed
                failureHandler.handleFailedResponse(response)
            }
            return handler.apply(response)
        }
    }

    /**
     * Simplifies GET request to return a string
     */
    fun getString(path: String, query: String? = null): String {
        val url = url(path, query)
        val request = Request.Builder().url(url).build()
        return execute(request) { response ->
            response.body?.string()!!
        }
    }

    /**
     * Simplifies GET request to return a JSON object marshalled to the specified class
     */
    inline fun <reified T> get(path: String, query: String? = null): T {
        return Json.fromJson(getString(path, query))
    }

    /**
     * Simplifies POST request to return a JSON object marshalled to the specified class
     */
    inline fun <reified T> post(path: String, body: RequestBody): T? {
        val url = url(path)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        return execute(request) { response ->
            val bodyAsString = response.body?.string()
            if (bodyAsString.isNullOrEmpty()) {
                return@execute null
            }
            Json.fromJson(bodyAsString)
        }
    }

    /**
     * Simplifies POST of Json object to return a JSON object marshalled to the specified class
     */
    inline fun <reified T> postJson(path: String, body: Any): T? {
        return post(path, requestBody(Json.toJson(body)))
    }

    /**
     * Create a RequestBody from a string
     * Defaults to media type of application/json
     */
    fun requestBody(body: String, mediaType: String = MEDIA_TYPE_JSON): RequestBody {
        return body.toRequestBody(mediaType.toMediaType())
    }

    inline fun <reified T> put(path: String, body: RequestBody): T? {
        val url = url(path)
        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()
        return execute(request) { response ->
            val bodyAsString = response.body?.string()
            if (bodyAsString.isNullOrEmpty()) {
                return@execute null
            }
            Json.fromJson(bodyAsString)
        }
    }

    inline fun <reified T> putJson(path: String, body: Any): T? {
        return put(path, requestBody(Json.toJson(body)))
    }

    fun delete(path: String) {
        val url = url(path)
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()
        return execute(request) { _ ->
            // nothing to return
        }
    }

    /**
     * Patch
     */
    inline fun <reified T> patch(path: String, body: RequestBody): T? {
        val url = url(path)
        val request = Request.Builder()
            .url(url)
            .patch(body)
            .build()
        return execute(request) { response ->
            val bodyAsString = response.body?.string()
            if (bodyAsString.isNullOrEmpty()) {
                return@execute null
            }
            Json.fromJson(bodyAsString)
        }
    }

    /**
     * Patch with Json object
     */
    inline fun <reified T> patchJson(path: String, body: Any): T? {
        return patch(path, requestBody(Json.toJson(body)))
    }

}