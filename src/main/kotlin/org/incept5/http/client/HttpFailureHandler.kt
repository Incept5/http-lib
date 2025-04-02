package org.incept5.http.client

import okhttp3.Response

interface HttpFailureHandler {

    /**
     * Add metadata to the exception and throw it
     *
     * @throws Exception
     */
    fun handleFailedResponse(response: Response)

}