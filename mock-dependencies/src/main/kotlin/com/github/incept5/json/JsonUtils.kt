package com.github.incept5.json

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Mock implementation of JsonUtils for build purposes
 */
object JsonUtils {
    val objectMapper: ObjectMapper = ObjectMapper()
    
    fun toJson(obj: Any): String {
        return objectMapper.writeValueAsString(obj)
    }
    
    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return objectMapper.readValue(json, clazz)
    }
}