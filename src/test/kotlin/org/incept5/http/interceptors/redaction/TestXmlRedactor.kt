package org.incept5.http.interceptors.redaction

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class TestXmlRedactor : ShouldSpec({

    context("Test XML Redactor") {

        should("should redact xml request and response correctly") {


            val unredactedXml = """
                <root>
                    <name>John Doe</name>
                    <password>secret</password>
                </root>
            """.trimIndent()

            val redactElements = listOf("name", "password")

            val redactedXml = "<root><name>xxxx</name><password>xxxx</password></root>"

            val redactor = XmlRedactor()

            val redacted = redactor.redactElements(unredactedXml, redactElements)

            redacted shouldBe  redactedXml
        }

        should("should redact attribute based XML okay") {
            val unredactedXml = """<root att="foo"><name>John Doe</name><password>secret</password></root>"""

            val redactElements = listOf("name", "password")

            val redactedXml = "<root att=\"foo\"><name>xxxx</name><password>xxxx</password></root>"

            val redactor = XmlRedactor()

            val redacted = redactor.redactElements(unredactedXml, redactElements)

            redacted shouldBe  redactedXml
        }
    }
})