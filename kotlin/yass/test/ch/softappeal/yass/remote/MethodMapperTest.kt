package ch.softappeal.yass.remote

import ch.softappeal.yass.Tag
import org.junit.Test
import java.lang.reflect.Method
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class MethodMapperTest {
    @Test
    fun javaOneWay() {
        val mapper = SimpleMethodMapperFactory(JavaOneWay::class.java)
        assertTrue(mapper.map(0).oneWay)
        assertFalse(mapper.map(1).oneWay)
    }

    private interface OneWayResult {
        @OneWay
        fun test(): Int
    }

    @Test
    fun oneWayResult() = try {
        SimpleMethodMapperFactory(OneWayResult::class.java)
        fail()
    } catch (e: IllegalStateException) {
        assertEquals(
            "OneWay method 'public abstract int " +
                    "ch.softappeal.yass.remote.MethodMapperTest${'$'}OneWayResult.test()' must return void",
            e.message
        )
    }

    private interface OneWayException {
        @OneWay
        @Throws(RuntimeException::class)
        fun test()
    }

    @Test
    fun oneWayException() = try {
        SimpleMethodMapperFactory(OneWayException::class.java)
        fail()
    } catch (e: IllegalStateException) {
        assertEquals(
            "OneWay method 'public abstract void " +
                    "ch.softappeal.yass.remote.MethodMapperTest${'$'}OneWayException.test() " +
                    "throws java.lang.RuntimeException' must not throw exceptions",
            e.message
        )
    }

    private interface NameOverloading {
        fun test()
        fun test(s: String)
    }

    @Test
    fun nameOverloading() = try {
        SimpleMethodMapperFactory(NameOverloading::class.java)
        fail()
    } catch (e: IllegalStateException) {
        assertEquals(
            "methods 'public abstract void " +
                    "ch.softappeal.yass.remote.MethodMapperTest${'$'}NameOverloading.test(java.lang.String)' and " +
                    "'public abstract void ch.softappeal.yass.remote.MethodMapperTest${'$'}NameOverloading.test()' " +
                    "in contract 'interface ch.softappeal.yass.remote.MethodMapperTest${'$'}NameOverloading' " +
                    "are overloaded",
            e.message
        )
    }

    private interface TagOverloading {
        @Tag(123)
        fun a() = 1

        @Tag(123)
        fun b() = 1
    }

    @Test
    fun tagOverloading() = try {
        TaggedMethodMapperFactory(TagOverloading::class.java)
        fail()
    } catch (e: IllegalStateException) {
        assertTrue(
            e.message.equals(
                "tag 123 used for methods 'public abstract int " +
                        "ch.softappeal.yass.remote.MethodMapperTest${'$'}TagOverloading.a()' and " +
                        "'public abstract int ch.softappeal.yass.remote.MethodMapperTest${'$'}TagOverloading.b()' " +
                        "in contract 'interface ch.softappeal.yass.remote.MethodMapperTest${'$'}TagOverloading'"
            )
                    || e.message.equals(
                "tag 123 used for methods 'public abstract int " +
                        "ch.softappeal.yass.remote.MethodMapperTest${'$'}TagOverloading.b()' and " +
                        "'public abstract int ch.softappeal.yass.remote.MethodMapperTest${'$'}TagOverloading.a()' " +
                        "in contract 'interface ch.softappeal.yass.remote.MethodMapperTest${'$'}TagOverloading'"
            )
        )
    }

    private interface TestService {
        @Tag(0)
        fun divide(a: Int, b: Int): Int

        @Tag(1)
        fun nothing()

        @Tag(2)
        @OneWay
        fun oneWay(sleepMillis: Int)
    }

    private fun methodMapperFactory(methodMapperFactory: MethodMapperFactory) {
        methodMapperFactory.mappings(TestService::class.java).forEach(::println)
        assertEquals(3, methodMapperFactory.mappings(TestService::class.java).count())
        val mapper = methodMapperFactory(TestService::class.java)
        fun check(method: Method, id: Int, oneWay: Boolean) {
            val mapping = mapper.map(method)
            assertEquals(method, mapping.method)
            assertEquals(id, mapping.id)
            assertEquals(oneWay, mapping.oneWay)
            assertEquals(mapping, mapper.map(id))
        }
        check(
            TestService::class.java.getMethod(
                "divide", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            ),
            0,
            false
        )
        check(TestService::class.java.getMethod("nothing"), 1, false)
        check(TestService::class.java.getMethod("oneWay", Int::class.javaPrimitiveType), 2, true)
        try {
            mapper.map(999)
            fail()
        } catch (e: IllegalStateException) {
            assertEquals(
                "unexpected method id 999 for contract " +
                        "'interface ch.softappeal.yass.remote.MethodMapperTest${'$'}TestService'",
                e.message
            )
        }
        try {
            mapper.map(TagOverloading::class.java.getMethod("a"))
            fail()
        } catch (e: IllegalStateException) {
            assertEquals(
                "unexpected method " +
                        "'public abstract int ch.softappeal.yass.remote.MethodMapperTest${'$'}TagOverloading.a()' " +
                        "for contract 'interface ch.softappeal.yass.remote.MethodMapperTest${'$'}TestService'",
                e.message
            )
        }
    }

    @Test
    fun simpleMethodMapperFactory() =
        methodMapperFactory(SimpleMethodMapperFactory)

    @Test
    fun taggedMethodMapperFactory() =
        methodMapperFactory(TaggedMethodMapperFactory)
}
