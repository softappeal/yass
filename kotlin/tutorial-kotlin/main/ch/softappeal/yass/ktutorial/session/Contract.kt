package ch.softappeal.yass.ktutorial.session

import ch.softappeal.yass.remote.*

class DivisionByZeroException : RuntimeException()

interface Calculator {
    suspend fun add(a: Int, b: Int): Int
    suspend fun divide(a: Int, b: Int): Int
}

interface EchoService {
    suspend fun echo(value: Any?): Any?
}

enum class WeatherType {
    Sunny,
    Rainy
}

data class Weather(
    val temperature: Int,
    val type: WeatherType
)

interface WeatherListener {
    @OneWay
    suspend fun update(weather: Weather)
}
