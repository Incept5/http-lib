package org.incept5.http.gateway

import org.incept5.http.auth.AuthTokenFetcher
import org.incept5.http.client.DefaultHttpFailureHandler
import org.incept5.http.client.HttpClient
import org.incept5.http.client.HttpFailureHandler
import org.incept5.http.interceptors.JsonLoggingInterceptor
import org.incept5.http.interceptors.RetryInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Deprecated: Use HttpClient instead
 *
 */
@Deprecated("Use HttpClient instead")
abstract class AbstractHttpGateway (
    baseUri: String,
    failureHandler: HttpFailureHandler = DefaultHttpFailureHandler(),
    tokenFetcher: AuthTokenFetcher? = null,
    retryInterceptor: Interceptor? = RetryInterceptor(),
    loggingInterceptor: Interceptor? = JsonLoggingInterceptor(), // json logging by default
    interceptors: List<Interceptor> = emptyList(),
    clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
) : HttpClient (baseUri, failureHandler, tokenFetcher, retryInterceptor, loggingInterceptor, interceptors, clientBuilder)