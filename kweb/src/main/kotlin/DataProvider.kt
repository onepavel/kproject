package org.example

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

class DataProvider(resourceName: String, private val skipHeader: Boolean = true) {
    private val lines: List<String>
    private val index = AtomicInteger(0)

    init {
        val stream = Thread.currentThread()
            .contextClassLoader
            .getResourceAsStream(resourceName)
                ?: error("Resource not found: $resourceName")
        stream.use { input ->
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                val all = reader.readLines()
                val data = if (skipHeader) all.drop(1) else all
                if (data.isEmpty()) {
                    error("No data lines found in resource: $resourceName")
                }
                lines = data.reversed()
            }
        }
    }

    fun nextLine(): String {
        val i = index.getAndIncrement()
        //val pos = Math.floorMod(i, lines.size)
        val pos = floorMod(i, lines.size)
        return lines[pos]
    }

    fun floorMod(x: Int, y: Int): Int {
        val r = x % y
        if ((x xor y) < 0 && r != 0) {
            return r + y
        }
        return r
    }
}