package org.example

private enum class Mode {
    PLAIN, KTOR
}

fun main(args: Array<String>) {
    val mode = if (true) Mode.PLAIN else Mode.KTOR
    val port = 8080

    val provider = DataProvider("sber.csv", skipHeader = true)

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Server stopped !!!")
    })

    println("Plain HTTP server started on http://localhost:${port}/next")
    println("Plain HTTP shutdown endpoint: http://localhost:${port}/shutdown")

    when (mode) {
        Mode.PLAIN -> startPlainServer(port, provider)
        Mode.KTOR -> startKtorServer(port, provider)
    }
}


//val config = Config(mode = Mode.PLAIN, port = 8080)

//private data class Config(
//    val mode: Mode,
//    val port: Int,
//)

//private fun parseArgs(args: Array<String>): Config {
//    var mode = Mode.KTOR
//    var port = 8080
//
//    args.forEach { arg ->
//        when {
//            arg == "plain" -> mode = Mode.PLAIN
//            arg == "ktor" -> mode = Mode.KTOR
//
//            arg.startsWith("--mode=") -> {
//                val value = arg.substringAfter("=").lowercase()
//                mode = when (value) {
//                    "plain" -> Mode.PLAIN
//                    "ktor" -> Mode.KTOR
//                    else -> error("Unknown mode: $value")
//                }
//            }
//
//            arg.startsWith("--port=") -> {
//                port = arg.substringAfter("=").toIntOrNull()
//                    ?: error("Invalid port: ${arg.substringAfter("=")}")
//            }
//        }
//    }
//
//    return Config(mode = mode, port = port)
//}