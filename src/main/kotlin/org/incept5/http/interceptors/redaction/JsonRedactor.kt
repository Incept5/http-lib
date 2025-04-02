package org.incept5.http.interceptors.redaction

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Redact elements from JSON content
 *
 */
class JsonRedactor {
    private val mapper = ObjectMapper()

    fun redactElements(content: String, redactElements: List<String>): JsonNode {
        try {
            val redactedJson = when (val jsonNode = mapper.readTree(content)) {
                is ObjectNode -> redactJsonObject(jsonNode, redactElements)
                is ArrayNode -> redactJsonArray(jsonNode, redactElements)
                else -> jsonNode
            }
            return redactedJson
        } catch (e: Exception) {
            throw RuntimeException("Error redacting JSON: $content", e)
        }
    }

    private fun redactJsonNode(jsonNode: JsonNode, redactElements: List<String>): JsonNode {
        return when (jsonNode) {
            is ObjectNode -> redactJsonObject(jsonNode, redactElements)
            is ArrayNode -> redactJsonArray(jsonNode, redactElements)
            else -> jsonNode
        }
    }

    private fun redactJsonObject(objectNode: ObjectNode, redactElements: List<String>): ObjectNode {
        val fieldNames = objectNode.fieldNames()
        for (fieldName in fieldNames) {
            val jsonNode = objectNode.get(fieldName)
            if (redactElements.contains(fieldName)) {
                objectNode.put(fieldName, "xxxx")
            } else {
                val redactedNode = redactJsonNode(jsonNode, redactElements)
                objectNode.set<JsonNode>(fieldName, redactedNode)
            }
        }
        return objectNode
    }

    private fun redactJsonArray(arrayNode: ArrayNode, redactElements: List<String>): ArrayNode {
        for (i in 0 until arrayNode.size()) {
            val jsonNode = arrayNode.get(i)
            val redactedNode = redactJsonNode(jsonNode, redactElements)
            arrayNode.set(i, redactedNode)
        }
        return arrayNode
    }
}