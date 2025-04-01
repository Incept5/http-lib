package com.github.incept5.correlation

/**
 * Mock implementation of CorrelationId for build purposes
 */
object CorrelationId {
    fun current(): String {
        return "mock-correlation-id"
    }
    
    fun withId(id: String, block: () -> Unit) {
        block()
    }
}