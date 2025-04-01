package org.incept5.http.interceptors

import com.fasterxml.jackson.databind.ObjectMapper
import org.incept5.http.interceptors.redaction.JsonRedactor
import org.incept5.http.interceptors.redaction.RedactConfig
import org.incept5.http.interceptors.redaction.XmlRedactor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Log request and response as JSON structures in the same style as Logbook
 *
 * Supports redacting sensitive information from the request and response
 * as long as the content type is either json or xml
 *
 * To enable logging you need to turn on the trace level for the logger
 *
 * org.incept5.http.interceptors: TRACE
 *
 * To bypass redaction when testing you can instead use this logger:
 *
 * org.incept5.http.interceptors-without-redaction: TRACE
 *
 */
class JsonLoggingInterceptor(
    private val redactConfig: RedactConfig = RedactConfig(),
) : Interceptor {

    private var log = LoggerFactory.getLogger(JsonLoggingInterceptor::class.java)
    private var logWithoutRedaction = LoggerFactory.getLogger(JsonLoggingInterceptor::class.java.packageName + "-without-redaction")

    private val jsonRedactor = JsonRedactor()
    private val xmlRedactor = XmlRedactor()
    private val objectMapper = ObjectMapper()

    override fun intercept(chain: Interceptor.Chain): Response {

        // only do our thing if trace is enabled for our logger
        if ( !log.isTraceEnabled && !logWithoutRedaction.isTraceEnabled) {
            // not tracing so no logging required so just pass the request through
            return chain.proceed(chain.request())
        }

        val request = chain.request()

        val id = UUID.randomUUID().toString().substring(24)

        // always redact Authorization header
        if (!redactConfig.headers.contains("Authorization")) {
            redactConfig.headers.add(0, "Authorization")
        }

        // Log request
        val requestLog = mutableMapOf<String, Any>()
        requestLog["id"] = id
        requestLog["type"] = "request"
        requestLog["method"] = request.method
        if ( logWithoutRedaction.isTraceEnabled ){
            requestLog["url"] = request.url.toString()
            requestLog["headers"] = request.headers.toMap().toMutableMap()
        }
        else {
            requestLog["url"] = redactQueryParams(request.url.toString(), redactConfig.queryParams)
            requestLog["headers"] = request.headers.toMap().toMutableMap().apply {
                redactConfig.headers.forEach { header ->
                    if (containsKey(header)) {
                        put(header, "xxxx")
                    }
                }
            }
        }

        val requestBody = request.body
        if (requestBody != null) {
            val buffer = Buffer()
            requestBody.writeTo(buffer)
            val bodyString = buffer.readUtf8()
            val contentType = requestBody.contentType()
            if (!logWithoutRedaction.isTraceEnabled && contentType != null) {
                when {
                    contentType.subtype.equals("json", ignoreCase = true) -> {
                        try {
                            val redactedBody = jsonRedactor.redactElements(bodyString, redactConfig.requestBodyElements)
                            requestLog["body"] = redactedBody
                        } catch (e : Exception){
                            log.warn ("Failed to redact json request body: {}", requestBody)
                            requestLog["body"] = bodyString
                        }
                    }

                    contentType.subtype.equals("xml", ignoreCase = true) -> {
                        try{
                            val redactedBody = xmlRedactor.redactElements(bodyString, redactConfig.requestBodyElements)
                            requestLog["body"] = redactedBody
                        } catch (e : Exception) {
                            log.warn("Failed to redact xml request body: {}", requestBody)
                            requestLog["body"] = bodyString
                        }
                    }
                }
            }

            if ( requestLog["body"] == null ) {
                if ( logWithoutRedaction.isTraceEnabled ) {
                    // not redacting
                    requestLog["body"] = bodyString
                }
                else if (!containsAny(bodyString, redactConfig.requestBodyElements) ){
                    // okay to log the unredacted body as it doesn't contain any redacted elements
                    requestLog["body"] = bodyString
                }
                else {
                    requestLog["body"] = "xxxx unable to redact xxxx"
                }
            }
        }

        val requestJson = objectMapper.writeValueAsString(requestLog)
        if ( logWithoutRedaction.isTraceEnabled ) {
            logWithoutRedaction.trace("HTTP request : {}", requestJson)
        }
        else {
            log.trace("HTTP request : {}", requestJson)
        }

        val startTime = System.currentTimeMillis()

        val response = chain.proceed(request)

        // Log response
        val responseLog = mutableMapOf<String, Any>()
        responseLog["id"] = id
        responseLog["type"] = "response"
        responseLog["status"] = response.code
        if ( logWithoutRedaction.isTraceEnabled ){
            responseLog["headers"] = response.headers.toMap().toMutableMap()
        }
        else {
            responseLog["headers"] = response.headers.toMap().toMutableMap().apply {
                redactConfig.headers.forEach { header ->
                    if (containsKey(header)) {
                        put(header, "xxxx")
                    }
                }
            }
        }
        responseLog["duration"] = System.currentTimeMillis() - startTime

        val responseBody = response.body
        if (responseBody != null) {
            val source = responseBody.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer
            val bodyString = buffer.clone().readUtf8()
            val contentType = responseBody.contentType()
            if (!logWithoutRedaction.isTraceEnabled && contentType != null) {
                when {
                    contentType.subtype.equals("json", ignoreCase = true) -> {
                        try {
                            val redactedBody = jsonRedactor.redactElements(bodyString, redactConfig.responseBodyElements)
                            responseLog["body"] = redactedBody
                        }
                        catch (e : Exception){
                            log.warn ("Failed to redact json response body: {}", responseBody)
                            responseLog["body"] = bodyString
                        }
                    }
                    contentType.subtype.equals("xml", ignoreCase = true) -> {
                        try{
                            val redactedBody = xmlRedactor.redactElements(bodyString, redactConfig.responseBodyElements)
                            responseLog["body"] = redactedBody
                        }
                        catch (e : Exception) {
                            log.warn("Failed to redact xml response body: {}", responseBody)
                            responseLog["body"] = bodyString
                        }
                    }
                }
            }

            if ( responseLog["body"] == null ) {
                if ( logWithoutRedaction.isTraceEnabled ){
                    // not redacting
                    responseLog["body"] = bodyString
                }
                else if (!containsAny(bodyString, redactConfig.responseBodyElements) ){
                    // okay to log the unredacted body as it doesn't contain any redacted elements
                    responseLog["body"] = bodyString
                }
                else {
                    responseLog["body"] = "xxxx unable to redact xxxx"
                }
            }
        }

        val responseJson = objectMapper.writeValueAsString(responseLog)
        if ( logWithoutRedaction.isTraceEnabled ){
            logWithoutRedaction.trace("HTTP response : {}", responseJson)
        }
        else {
            log.trace("HTTP response : {}", responseJson)
        }

        return response
    }

    private fun containsAny( bodyString: String, elements: List<String> ) : Boolean {
        elements.forEach { element ->
            if (bodyString.contains(element)) {
                return true
            }
        }
        return false
    }

    private fun redactQueryParams(url: String, queryParamsToRedact: List<String>): String {
        val urlBuilder = url.toHttpUrlOrNull()?.newBuilder()
        queryParamsToRedact.forEach { param ->
            urlBuilder?.setQueryParameter(param, "xxxx")
        }
        return urlBuilder?.build()?.toString() ?: url
    }

    // for testability
    fun setLogger(logger: Logger, loggerWithoutRedaction: Logger? = null) {
        log = logger
        if ( loggerWithoutRedaction != null ){
            logWithoutRedaction = loggerWithoutRedaction
        }
    }
}

