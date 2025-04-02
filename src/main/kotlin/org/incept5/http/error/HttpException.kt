package org.incept5.http.error

import org.incept5.error.CoreException
import org.incept5.error.Error
import org.incept5.error.ErrorCategory

/**
 * Http Exception
 */
open class HttpException(category: ErrorCategory, error: HttpErrors, msg : String, retryable : Boolean = false) : CoreException(category, listOf(Error(error.getCode())), msg, null, retryable )