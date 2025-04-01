package org.incept5.http.client

import org.incept5.error.ErrorCategory
import org.incept5.http.error.HttpRequestFailedException
import org.incept5.telemetry.log.LogEvent
import okhttp3.Response
import org.slf4j.LoggerFactory

/**
 * Default implementation of the HttpFailureHandler
 * which will throw an exception based on the response code
 */
class DefaultHttpFailureHandler : HttpFailureHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultHttpFailureHandler::class.java)
    }

    override fun handleFailedResponse(response: Response) {
        if ( !response.isSuccessful ) {
            logger.warn ( "HTTP call failed : {}", LogEvent("httpStatusCode" to response.code, "body" to response.body?.string()) )
            val category = when (response.code) {
                401 -> ErrorCategory.AUTHENTICATION
                403 -> ErrorCategory.AUTHORIZATION
                404 -> ErrorCategory.NOT_FOUND
                409 -> ErrorCategory.CONFLICT
                else -> ErrorCategory.UNEXPECTED
            }
            val message = "http request failed with code: ${response.code}"
            // retry on 409 and 5XX
            val retryable = response.code == 409 || response.code >= 500
            throw HttpRequestFailedException(category, message, retryable)
        }
    }
}