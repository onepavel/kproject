package org.example

import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

lateinit var engine: NettyApplicationEngine

fun startKtorServer(port: Int, provider: DataProvider) {
    engine = embeddedServer(Netty, port) {
        routing {
            get("/next") {
                call.respondText(provider.nextLine(), ContentType.Text.Plain)
            }
            get("/shutdown") { //post
                call.respondText("Shutting down", ContentType.Text.Plain)
                Thread {
                    Thread.sleep(50)
                    engine.stop(0, 5000)
                    println("Ktor server engine stopped")
                }.start()
            }
        }
    }
    engine.start(wait = true)
}