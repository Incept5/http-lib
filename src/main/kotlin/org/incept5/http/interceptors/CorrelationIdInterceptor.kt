package org.incept5.http.interceptors

import org.incept5.correlation.CorrelationId
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Set X-Correlation-ID header in each request
 */
class CorrelationIdInterceptor(val headerName: String = "X-Correlation-ID") : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val correlationId = CorrelationId.getId()
        val newRequest = chain.request().newBuilder()
            .addHeader(headerName, correlationId)
            .build()
        return chain.proceed(newRequest)
    }
}