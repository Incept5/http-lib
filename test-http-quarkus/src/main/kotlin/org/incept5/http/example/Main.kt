package org.incept5.http.example
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.annotations.QuarkusMain

@QuarkusMain
class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            println("Running main method")
            Quarkus.run(*args)
        }
    }
}