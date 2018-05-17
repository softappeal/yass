package ch.softappeal.yass

import ch.softappeal.yass.serialize.contractj.FieldModifiers
import ch.softappeal.yass.serialize.contractj.NoDefaultConstructor
import ch.softappeal.yass.serialize.contractj.nested.AllTypes
import org.junit.Test
import java.lang.reflect.Field
import java.util.Arrays
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReflectTest {

    @Test
    fun noDefaultConstructor() {
        val instance = ALLOCATE(NoDefaultConstructor::class.java)() as NoDefaultConstructor
        assertTrue(instance.i == 0)
    }

    private fun name2field(type: Class<*>): Map<String, Field> {
        return allFields(type).associateBy(Field::getName, { it })
    }

    @Test
    fun fieldModifiers() {
        FieldModifiers.CONSTRUCTOR_CALLED = false
        val fieldModifiers = ALLOCATE(FieldModifiers::class.java)() as FieldModifiers
        assertFalse(FieldModifiers.CONSTRUCTOR_CALLED)
        val name2field = name2field(FieldModifiers::class.java)
        val privateField = name2field["privateField"]!!
        val privateFinalField = name2field["privateFinalField"]!!
        val publicField = name2field["publicField"]!!
        val publicFinalField = name2field["publicFinalField"]!!
        assertTrue(fieldModifiers.publicFinalField == 0)
        assertTrue(publicFinalField.get(fieldModifiers) as Int == 0)
        assertTrue(privateFinalField.get(fieldModifiers) as Int == 0)
        privateField.set(fieldModifiers, 200)
        privateFinalField.set(fieldModifiers, 201)
        publicField.set(fieldModifiers, 202)
        publicFinalField.set(fieldModifiers, 203)
        assertTrue(fieldModifiers.publicField == 202)
        assertTrue(fieldModifiers.publicFinalField == 203)
        assertTrue(privateField.get(fieldModifiers) as Int == 200)
        assertTrue(privateFinalField.get(fieldModifiers) as Int == 201)
        assertTrue(publicField.get(fieldModifiers) as Int == 202)
        assertTrue(publicFinalField.get(fieldModifiers) as Int == 203)
    }

    @Test
    fun allTypes() {
        val allTypes = ALLOCATE(AllTypes::class.java)() as AllTypes
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
        assertTrue(allTypes.byteField.toInt() == 123)
        assertTrue(allTypes.shortField.toInt() == 12345)
        assertTrue(allTypes.intField == 123456789)
        assertTrue(allTypes.longField == 123456789000L)
        assertTrue(allTypes.charField == 'x')
        assertTrue(allTypes.floatField == 1.23f)
        assertTrue(allTypes.doubleField == 3.21)
        assertTrue(booleanField.get(allTypes) as Boolean)
        assertTrue(byteField.get(allTypes) as Byte == 123.toByte())
        assertTrue(shortField.get(allTypes) as Short == 12345.toShort())
        assertTrue(intField.get(allTypes) as Int == 123456789)
        assertTrue(longField.get(allTypes) as Long == 123456789000L)
        assertTrue(charField.get(allTypes) as Char == 'x')
        assertTrue(floatField.get(allTypes) as Float == 1.23f)
        assertTrue(doubleField.get(allTypes) as Double == 3.21)
        val stringField = name2field["stringField"]!!
        stringField.set(allTypes, "xyz")
        assertEquals("xyz", allTypes.stringField)
        assertEquals("xyz", stringField.get(allTypes))
        val objectListField = name2field["objectListField"]!!
        objectListField.set(allTypes, Arrays.asList("xyz", 42, null))
        assertTrue(Arrays.asList("xyz", 42, null) == allTypes.objectListField)
        assertTrue(Arrays.asList("xyz", 42, null) == objectListField.get(allTypes))
    }

}
