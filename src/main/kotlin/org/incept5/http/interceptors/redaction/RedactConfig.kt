package org.incept5.http.interceptors.redaction

data class RedactConfig(
    val queryParams: List<String> = emptyList(),
    val headers: MutableList<String> = mutableListOf(),
    val requestBodyElements: List<String> = emptyList(),
    val responseBodyElements: List<String> = emptyList()
)