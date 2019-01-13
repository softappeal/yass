package ch.softappeal.yass

import ch.softappeal.yass.serialize.*
import ch.softappeal.yass.serialize.nested.*
import java.lang.reflect.*
import kotlin.test.*

private fun name2field(type: Class<*>): Map<String, Field> =
    type.allFields.associateBy(Field::getName)

class NoDefaultConstructor private constructor(val i: Int)

class ReflectTest {
    @Test
    fun noDefaultConstructor() {
        val instance = AllocatorFactory(NoDefaultConstructor::class.java)() as NoDefaultConstructor
        assertEquals(instance.i, 0)
    }

    @Test
    fun fieldModifiers() {
        CONSTRUCTOR_CALLED = false
        val fieldModifiers = AllocatorFactory(FieldModifiers::class.java)() as FieldModifiers
        assertFalse(CONSTRUCTOR_CALLED)
        val name2field = name2field(FieldModifiers::class.java)
        val privateField = name2field["privateField"]!!
        val privateFinalField = name2field["privateFinalField"]!!
        val publicField = name2field["publicField"]!!
        val publicFinalField = name2field["publicFinalField"]!!
        assertEquals(0, fieldModifiers.publicFinalField)
        assertEquals(0, publicFinalField.get(fieldModifiers) as Int)
        assertEquals(0, privateFinalField.get(fieldModifiers) as Int)
        privateField.set(fieldModifiers, 200)
        privateFinalField.set(fieldModifiers, 201)
        publicField.set(fieldModifiers, 202)
        publicFinalField.set(fieldModifiers, 203)
        assertEquals(202, fieldModifiers.publicField)
        assertEquals(203, fieldModifiers.publicFinalField)
        assertEquals(200, privateField.get(fieldModifiers) as Int)
        assertEquals(201, privateFinalField.get(fieldModifiers) as Int)
        assertEquals(202, publicField.get(fieldModifiers) as Int)
        assertEquals(203, publicFinalField.get(fieldModifiers) as Int)
    }

    @Test
    fun allTypes() {
        val allTypes = AllocatorFactory(AllTypes::class.java)() as AllTypes
        val name2field = name2field(AllTypes::class.java)
        val booleanField = name2field["booleanField"]!!
        val shortField = name2field["shortField"]!!
        val intField = name2field["intField"]!!
        val longField = name2field["longField"]!!
        booleanField.set(allTypes, true)
        shortField.set(allTypes, 12345.toShort())
        intField.set(allTypes, 123456789)
        longField.set(allTypes, 123456789000L)
        assertTrue(allTypes.booleanField)
        assertEquals(12345, allTypes.shortField.toInt())
        assertEquals(123456789, allTypes.intField)
        assertEquals(123456789000L, allTypes.longField)
        assertTrue(booleanField.get(allTypes) as Boolean)
        assertEquals(12345.toShort(), shortField.get(allTypes) as Short)
        assertEquals(123456789, intField.get(allTypes) as Int)
        assertEquals(123456789000L, longField.get(allTypes) as Long)
        val stringField = name2field["stringField"]!!
        stringField.set(allTypes, "xyz")
        assertEquals("xyz", allTypes.stringField)
        assertEquals("xyz", stringField.get(allTypes))
        val objectListField = name2field["objectListField"]!!
        objectListField.set(allTypes, listOf("xyz", 42, null))
        assertEquals(listOf("xyz", 42, null), allTypes.objectListField)
        assertEquals(listOf("xyz", 42, null), objectListField.get(allTypes))
    }
}
