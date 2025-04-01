package org.incept5.http.example

import java.util.UUID

data class ExamplePayload(val id: UUID, val name: String, val age: Int = 23)
