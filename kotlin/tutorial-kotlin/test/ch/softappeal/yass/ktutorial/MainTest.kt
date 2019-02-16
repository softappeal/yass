package ch.softappeal.yass.ktutorial

import io.ktor.util.*
import kotlin.test.*

class MainTest {
    @KtorExperimentalAPI
    @Test
    fun helloworld() = try {
        ch.softappeal.yass.ktutorial.helloworld.main()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    @Test
    fun contextmessageserializer() {
        ch.softappeal.yass.ktutorial.contextmessageserializer.main()
    }

    @KtorExperimentalAPI
    @Test
    fun scontextmessageserializer() = try {
        ch.softappeal.yass.ktutorial.scontextmessageserializer.main()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
