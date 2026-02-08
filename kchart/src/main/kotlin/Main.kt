@file:OptIn(ExperimentalComposeUiApi::class)

package org.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                isMinimized = false,
                placement = WindowPlacement.Floating,
                position = WindowPosition.PlatformDefault,
                size = DpSize(800.dp, 600.dp),
            )
        ) {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    MaterialTheme {
        val candles = remember { mutableStateListOf<Candle>() }

        LaunchedEffect(Unit) {
            while (isActive) {
                // Запрос одной строки данных раз в секунду
                val line = fetchNextLine()
                if (line != null) {
                    parseCandle(line)?.let { candle ->
                        candles.add(candle)
                        // Ограничиваем буфер, чтобы не раздувать память
                        if (candles.size > 300) {
                            candles.removeRange(0, candles.size - 300)
                        }
                    }
                }
                delay(1000)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF101418))
        ) {
            CandleChart(candles = candles)
        }
    }
}

@Composable
private fun CandleChart(candles: List<Candle>) {
    var zoom by remember { mutableStateOf(1.0f) }
    var scrollIndex by remember { mutableStateOf(0) }
    var autoFollow by remember { mutableStateOf(true) }

    LaunchedEffect(candles.size, zoom, autoFollow) {
        if (candles.isEmpty()) return@LaunchedEffect
        val baseVisible = 80
        // Количество свечей в окне зависит от масштаба
        val visible = (baseVisible / zoom).toInt().coerceAtLeast(10)
        val maxStart = (candles.size - visible).coerceAtLeast(0)
        if (autoFollow) {
            // Автоскролл к последним данным
            scrollIndex = maxStart
        } else {
            scrollIndex = scrollIndex.coerceIn(0, maxStart)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Scroll) { event ->
                // Колесо мыши: зум
                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                if (delta != 0f) {
                    autoFollow = false
                    val factor = if (delta < 0f) 1.1f else 0.9f
                    zoom = (zoom * factor).coerceIn(0.5f, 6.0f)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { autoFollow = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (candles.isEmpty()) return@detectDragGestures
                        val baseVisible = 80
                        val visible = (baseVisible / zoom).toInt().coerceAtLeast(10)
                        val maxStart = (candles.size - visible).coerceAtLeast(0)
                        // Перетаскивание: горизонтальный скролл
                        val candlesPerPixel = visible / size.width.coerceAtLeast(1)
                        val delta = (dragAmount.x * candlesPerPixel).toInt()
                        scrollIndex = (scrollIndex - delta).coerceIn(0, maxStart)
                    }
                )
            }
    ) {
        if (candles.isEmpty()) return@Canvas

        val baseVisible = 80
        val visible = (baseVisible / zoom).toInt().coerceAtLeast(10)
        val maxStart = (candles.size - visible).coerceAtLeast(0)
        val startIndex = scrollIndex.coerceIn(0, maxStart)
        val endIndex = (startIndex + visible).coerceAtMost(candles.size)
        // Текущая видимая часть данных
        val view = candles.subList(startIndex, endIndex)

        val maxPrice = view.maxOf { it.high }
        val minPrice = view.minOf { it.low }
        val priceRange = (maxPrice - minPrice).takeIf { it > 0f } ?: 1f

        val gap = 2f
        val candleWidth = ((size.width - gap) / view.size).coerceAtLeast(3f)
        val chartHeight = size.height

        fun yFor(price: Float): Float {
            val normalized = (price - minPrice) / priceRange
            return chartHeight - (normalized * chartHeight)
        }

        // Фоновая сетка
        drawGrid(
            width = size.width,
            height = size.height,
            rows = 6,
            cols = 8,
            color = Color(0xFF1C2329)
        )

        view.forEachIndexed { index, c ->
            val x = index * candleWidth + gap

            val wickTop = yFor(c.high)
            val wickBottom = yFor(c.low)
            val openY = yFor(c.open)
            val closeY = yFor(c.close)

            val isUp = c.close >= c.open
            val color = if (isUp) Color(0xFF3FB950) else Color(0xFFF85149)

            // Фитиль свечи
            drawLine(
                color = color,
                start = Offset(x + candleWidth / 2f, wickTop),
                end = Offset(x + candleWidth / 2f, wickBottom),
                strokeWidth = 1.5f
            )

            val bodyTop = minOf(openY, closeY)
            val bodyBottom = maxOf(openY, closeY)
            val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(1.5f)

            // Тело свечи
            drawRect(
                color = color,
                topLeft = Offset(x, bodyTop),
                size = Size(candleWidth - gap, bodyHeight)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(
    width: Float,
    height: Float,
    rows: Int,
    cols: Int,
    color: Color
) {
    val rowStep = height / rows
    val colStep = width / cols

    for (r in 0..rows) {
        val y = r * rowStep
        drawLine(color = color, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
    }
    for (c in 0..cols) {
        val x = c * colStep
        drawLine(color = color, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
    }
}

private data class Candle(
    val date: LocalDate,
    val close: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val volume: Float,
    val changePercent: Float,
)

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

private suspend fun fetchNextLine(): String? = withContext(Dispatchers.IO) {
    runCatching {
        // Запрос данных с локального сервера
        URL("http://localhost:8080/next").readText().trim()
    }.getOrNull()
}

private fun parseCandle(line: String): Candle? {
    val parts = line.split(",").map { it.trim().trim('"') }
    if (parts.size < 7) return null

    fun parseNumber(value: String): Float? {
        // Числа приходят с запятой и суффиксами: %, M
        val cleaned = value
            .replace("%", "")
            .replace("M", "")
            .replace(" ", "")
            .replace(',', '.')
        return cleaned.toFloatOrNull()
    }

    val date = runCatching { LocalDate.parse(parts[0], dateFormatter) }.getOrNull() ?: return null
    val close = parseNumber(parts[1]) ?: return null
    val open = parseNumber(parts[2]) ?: return null
    val high = parseNumber(parts[3]) ?: return null
    val low = parseNumber(parts[4]) ?: return null
    val volumeRaw = parts[5]
    val volume = parseNumber(volumeRaw)?.let { v ->
        // Объем может быть в миллионах (суффикс M)
        if (volumeRaw.contains('M')) v * 1_000_000f else v
    } ?: return null
    val changePercent = parseNumber(parts[6]) ?: return null

    return Candle(date, close, open, high, low, volume, changePercent)
}
