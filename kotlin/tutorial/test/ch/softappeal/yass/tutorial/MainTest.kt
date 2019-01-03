package ch.softappeal.yass.tutorial

import ch.softappeal.yass.tutorial.py.*
import kotlin.test.*

class MainTest {
    @Test
    fun serializeTest() {
        SerializeTest.main()
    }

    @Test
    fun tutorial() {
        main()
    }

    @Test
    fun context() {
        ch.softappeal.yass.tutorial.context.main()
    }
}
