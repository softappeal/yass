package ch.softappeal.yass

import kotlin.test.*

class TagTest {
    @Tag(1)
    class TaggedClass(@Tag(2) var a: Int, @Tag(3) val b: Int) {
        @Tag(4)
        var x: Int = 0
        @Tag(5)
        val y: Int = 0

        @Tag(6)
        fun withTag() {
            // empty
        }

        fun noTag() {
            // empty
        }
    }

    @Test
    fun tags() {
        assertEquals(1, tag(TaggedClass::class.java))
        assertEquals(2, tag(TaggedClass::class.java.getDeclaredField("a")))
        assertEquals(3, tag(TaggedClass::class.java.getDeclaredField("b")))
        assertEquals(4, tag(TaggedClass::class.java.getDeclaredField("x")))
        assertEquals(5, tag(TaggedClass::class.java.getDeclaredField("y")))
        assertEquals(6, tag(TaggedClass::class.java.getMethod("withTag")))
        assertEquals(
            "missing tag for 'public final void ch.softappeal.yass.TagTest${'$'}TaggedClass.noTag()'",
            assertFailsWith<IllegalStateException> { tag(TaggedClass::class.java.getMethod("noTag")) }.message
        )
    }

    @Test
    fun javaTags() {
        assertEquals(1, tag(JavaTaggedClass::class.java))
        assertEquals(2, tag(JavaTaggedClass::class.java.getDeclaredField("tag")))
        assertEquals(3, tag(JavaTaggedClass::class.java.getMethod("withTag")))
        assertEquals(
            "missing tag for 'public void ch.softappeal.yass.JavaTaggedClass.noTag()'",
            assertFailsWith<IllegalStateException> { tag(JavaTaggedClass::class.java.getMethod("noTag")) }.message
        )
    }
}
