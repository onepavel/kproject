package org.example

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun startPlainServer(port: Int, provider: DataProvider) {
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.createContext("/next") { exchange ->
        handleNext(exchange, provider)
    }
    server.createContext("/shutdown") { exchange ->
        handleShutdown(exchange) {
            server.stop(0)
            (server.executor as? ExecutorService)?.shutdown()
        }
    }
    server.executor = Executors.newSingleThreadExecutor()
    server.start()
}

private fun handleNext(exchange: HttpExchange, provider: DataProvider) {
    try {
        if (exchange.requestMethod == "GET") {
            val body = provider.nextLine()
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use {
                it.write(bytes)
            }
        } else {
            exchange.sendResponseHeaders(405, -1)
        }
    } finally {
        exchange.close()
    }
}

fun handleShutdown(exchange: HttpExchange, shutdownAction: () -> Unit) {
    try {
        if (exchange.requestMethod == "GET") { //"POST"
            val body = "Shutting down"
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use {
                it.write(bytes)
            }
        } else {
            exchange.sendResponseHeaders(405, -1)
        }
    } finally {
        exchange.close()
    }
    Thread {
        Thread.sleep(50)
        shutdownAction()
    }.start()
}