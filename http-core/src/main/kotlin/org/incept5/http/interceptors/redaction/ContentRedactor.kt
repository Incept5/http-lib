package org.incept5.http.interceptors.redaction

interface ContentRedactor {
    fun redactElements(content: String, redactElements: List<String>): String
}