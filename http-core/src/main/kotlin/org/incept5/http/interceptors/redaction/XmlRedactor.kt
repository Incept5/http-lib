package org.incept5.http.interceptors.redaction

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Parse the xml string into a dom and redact the elements and then
 * spit out the normalised redacted XML without new lines etc
 */
class XmlRedactor : ContentRedactor {
    override fun redactElements(content: String, redactElements: List<String>): String {
        try {
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val inputSource = InputSource(StringReader(content))
            val document = documentBuilder.parse(inputSource)
            redactXmlElements(document.documentElement, redactElements)
            val writer = StringWriter()
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "no")
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            transformer.setOutputProperty(OutputKeys.METHOD, "xml")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            val result = StreamResult(writer)
            transformer.transform(DOMSource(document), result)
            // remove spaces between tags to compact the xml and unpretty print it
            return writer.toString().replace(">\\s+<".toRegex(), "><")
        } catch (e: Exception) {
            throw RuntimeException("Error redacting XML: $content", e)
        }
    }

    private fun redactXmlElements(element: Element, redactElements: List<String>) {
        if (redactElements.contains(element.tagName)) {
            element.textContent = "xxxx"
        }
        val childNodes: NodeList = element.childNodes
        for (i in 0 until childNodes.length) {
            val node: Node = childNodes.item(i)
            if (node.nodeType == Node.ELEMENT_NODE) {
                redactXmlElements(node as Element, redactElements)
            }
        }
    }
}