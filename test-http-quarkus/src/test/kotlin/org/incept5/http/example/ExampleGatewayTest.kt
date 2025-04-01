package org.incept5.http.example

import com.github.tomakehurst.wiremock.client.WireMock
import io.quarkiverse.wiremock.devservice.ConnectWireMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
@ConnectWireMock
class ExampleGatewayTest {

    val KNOWN_ID = UUID.fromString("08cb06af-bbbf-42c1-af48-5411b1deb388")

    @Inject
    lateinit var exampleGateway: ExampleGatewayWithAuth

    lateinit var wireMock: WireMock


    /**
     * The mapping files are located in src/test/resources/mappings
     */
    @Test
    fun `example gateway test using wiremock mappings file`() {
        val payload = exampleGateway.getSomethingById(KNOWN_ID)
        assert(payload.id == KNOWN_ID)
        assert(payload.name == "Joe Bloggs")
        assert(payload.age == 23)
    }

    @Test
    fun `example gateway test using wiremock stubs`() {

        val id = UUID.randomUUID()

        wireMock.register(
            WireMock.get(WireMock.urlEqualTo("/something/$id"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                                "id": "$id",
                                "name": "Mary Shelley",
                                "age": 42
                            }
                            """.trimIndent()
                        )
                )
        )

        val payload = exampleGateway.getSomethingById(id)
        assert(payload.id == id)
        assert(payload.name == "Mary Shelley")
        assert(payload.age == 42)
    }

}