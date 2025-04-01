package org.incept5.http.example

import java.util.*

data class ExamplePayload(val id: UUID, val name: String, val age: Int = 23, val components: Array<SubComponent> = emptyArray())


data class SubComponent(val secret: String)
