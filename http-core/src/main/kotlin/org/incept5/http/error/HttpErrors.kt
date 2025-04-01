package org.incept5.http.error

import org.incept5.error.ErrorCode

enum class HttpErrors(private val code: String) : ErrorCode {
    HTTP_RESPONSE_EMPTY("http.response.empty"),
    HTTP_REQUEST_FAILED("http.request.failed"),
    ;

    override fun getCode(): String {
        return this.code
    }
}