package org.incept5.http.example

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import kotlin.random.Random

@Path("/example")
class ExampleResource {

    val random: Random = Random(0)

    @GET
    fun getExample(): Response {
        val nextInt = random.nextInt(0, 10)
        if ( nextInt < 8 ) {
            return Response.ok("GOOD_RESPONSE").build()
        } else {
            return Response.status(Response.Status.CONFLICT).entity("Bad Response").build()
        }
    }
}