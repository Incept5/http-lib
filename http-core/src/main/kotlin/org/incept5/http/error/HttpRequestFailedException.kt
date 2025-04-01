package org.incept5.http.error

import org.incept5.error.ErrorCategory

/**
 * Thrown when we get a non 2xx response from the server
 */
class HttpRequestFailedException(category: ErrorCategory, msg: String, retryable: Boolean = false) : HttpException(category, HttpErrors.HTTP_REQUEST_FAILED, msg, retryable)