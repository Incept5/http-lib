package com.github.incept5.error

/**
 * Mock implementation of ApplicationException for build purposes
 */
open class ApplicationException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message ?: errorCode.message, cause)