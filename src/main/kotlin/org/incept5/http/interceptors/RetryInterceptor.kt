package org.incept5.http.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory


/**
 * Interceptor that retries the request if it fails
 *
 * Default policy is to retry 3 times on 409 or any 5xx error
 *
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryPolicy: RetryPolicy = StandardRetryPolicy(),
    private val pauseBetweenRetriesMs: Long = 500
) : Interceptor {

    companion object {
        private val logger = LoggerFactory.getLogger(RetryInterceptor::class.java)
    }

    /**
     * Retry on 409 or any 5xx error
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        var response = chain.proceed(request)
        var retryCount = 0
        while (retryPolicy.shouldRetry(response) && retryCount < maxRetries) {
            retryCount++
            logger.info ( """Retrying HTTP request :
                url ${request.url},
                responseCode ${response.code},
                retryCount ${retryCount},
                maxRetries  $maxRetries
                """
            )
            Thread.sleep(pauseBetweenRetriesMs)
            request = request.newBuilder().build()
            response.close()
            response = chain.proceed(request)
        }
        return response
    }
}

/**
 * You can implement other retry policies
 **/
interface RetryPolicy {
    fun shouldRetry(response: Response): Boolean
}

/**
 * Standard Retry Policy
 **/
class StandardRetryPolicy : RetryPolicy {

    /**
     * Retry on 409 or any 5xx error
     */
    override fun shouldRetry(response: Response): Boolean {
        return response.code == 409 || response.code in 500..599
    }
}