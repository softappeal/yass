package ch.softappeal.yass

import ch.softappeal.yass.serialize.FieldModifiers
import ch.softappeal.yass.serialize.NoDefaultConstructor
import ch.softappeal.yass.serialize.nested.AllTypes
import java.lang.reflect.Field
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun name2field(type: Class<*>): Map<String, Field> =
    allFields(type).associateBy(Field::getName) { it }

class ReflectTest {
    @Test
    fun noDefaultConstructor() {
        val instance = AllocatorFactory(NoDefaultConstructor::class.java)() as NoDefaultConstructor
        assertEquals(instance.i, 0)
    }

    @Test
    fun fieldModifiers() {
        FieldModifiers.CONSTRUCTOR_CALLED = false
        val fieldModifiers = AllocatorFactory(FieldModifiers::class.java)() as FieldModifiers
        assertFalse(FieldModifiers.CONSTRUCTOR_CALLED)
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
        val byteField = name2field["byteField"]!!
        val shortField = name2field["shortField"]!!
        val intField = name2field["intField"]!!
        val longField = name2field["longField"]!!
        val charField = name2field["charField"]!!
        val floatField = name2field["floatField"]!!
        val doubleField = name2field["doubleField"]!!
        booleanField.set(allTypes, true)
        byteField.set(allTypes, 123.toByte())
        shortField.set(allTypes, 12345.toShort())
        intField.set(allTypes, 123456789)
        longField.set(allTypes, 123456789000L)
        charField.set(allTypes, 'x')
        floatField.set(allTypes, 1.23f)
        doubleField.set(allTypes, 3.21)
        doubleField.set(allTypes, 3.21)
        assertTrue(allTypes.booleanField)
        assertEquals(123, allTypes.byteField.toInt())
        assertEquals(12345, allTypes.shortField.toInt())
        assertEquals(123456789, allTypes.intField)
        assertEquals(123456789000L, allTypes.longField)
        assertEquals('x', allTypes.charField)
        assertEquals(1.23f, allTypes.floatField)
        assertEquals(3.21, allTypes.doubleField)
        assertTrue(booleanField.get(allTypes) as Boolean)
        assertEquals(123.toByte(), byteField.get(allTypes) as Byte)
        assertEquals(12345.toShort(), shortField.get(allTypes) as Short)
        assertEquals(123456789, intField.get(allTypes) as Int)
        assertEquals(123456789000L, longField.get(allTypes) as Long)
        assertEquals('x', charField.get(allTypes) as Char)
        assertEquals(1.23f, floatField.get(allTypes) as Float)
        assertEquals(3.21, doubleField.get(allTypes) as Double)
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
