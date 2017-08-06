package ch.softappeal.yass.util.test;

import ch.softappeal.yass.serialize.contract.nested.AllTypes;
import ch.softappeal.yass.util.Reflect;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReflectTest {

    @Test public void noDefaultConstructor() {
        try {
            Reflect.constructor(NoDefaultConstructor.class);
            Assert.fail();
        } catch (final RuntimeException e) {
            System.out.println(e);
        }
    }

    private static Map<String, Field> name2field(final Class<?> type) {
        return Reflect.allFields(type).stream().collect(Collectors.toMap(Field::getName, Function.identity()));
    }

    @Test public void fieldModifiers() throws Exception {
        FieldModifiers.CONSTRUCTOR_CALLED = false;
        final FieldModifiers fieldModifiers = Reflect.constructor(FieldModifiers.class).newInstance();
        Assert.assertTrue(FieldModifiers.CONSTRUCTOR_CALLED);
        final Map<String, Field> name2field = name2field(FieldModifiers.class);
        final Field privateField = name2field.get("privateField");
        final Field privateFinalField = name2field.get("privateFinalField");
        final Field publicField = name2field.get("publicField");
        final Field publicFinalField = name2field.get("publicFinalField");
        Assert.assertTrue(fieldModifiers.publicFinalField == 101);
        Assert.assertTrue((Integer)publicFinalField.get(fieldModifiers) == 101);
        Assert.assertTrue((Integer)privateFinalField.get(fieldModifiers) == 100);
        privateField.set(fieldModifiers, 200);
        privateFinalField.set(fieldModifiers, 201);
        publicField.set(fieldModifiers, 202);
        publicFinalField.set(fieldModifiers, 203);
        Assert.assertTrue(fieldModifiers.publicField == 202);
        Assert.assertTrue(fieldModifiers.publicFinalField == 203);
        Assert.assertTrue((Integer)privateField.get(fieldModifiers) == 200);
        Assert.assertTrue((Integer)privateFinalField.get(fieldModifiers) == 201);
        Assert.assertTrue((Integer)publicField.get(fieldModifiers) == 202);
        Assert.assertTrue((Integer)publicFinalField.get(fieldModifiers) == 203);
    }

    @Test public void allTypes() throws Exception {
        final AllTypes allTypes = Reflect.constructor(AllTypes.class).newInstance();
        final Map<String, Field> name2field = name2field(AllTypes.class);
        final Field booleanField = name2field.get("booleanField");
        final Field byteField = name2field.get("byteField");
        final Field shortField = name2field.get("shortField");
        final Field intField = name2field.get("intField");
        final Field longField = name2field.get("longField");
        final Field charField = name2field.get("charField");
        final Field floatField = name2field.get("floatField");
        final Field doubleField = name2field.get("doubleField");
        booleanField.set(allTypes, true);
        byteField.set(allTypes, (byte)123);
        shortField.set(allTypes, (short)12345);
        intField.set(allTypes, 123456789);
        longField.set(allTypes, 123456789000L);
        charField.set(allTypes, 'x');
        floatField.set(allTypes, 1.23f);
        doubleField.set(allTypes, 3.21d);
        doubleField.set(allTypes, 3.21d);
        Assert.assertTrue(allTypes.booleanField == true);
        Assert.assertTrue(allTypes.byteField == 123);
        Assert.assertTrue(allTypes.shortField == 12345);
        Assert.assertTrue(allTypes.intField == 123456789);
        Assert.assertTrue(allTypes.longField == 123456789000L);
        Assert.assertTrue(allTypes.charField == 'x');
        Assert.assertTrue(allTypes.floatField == 1.23f);
        Assert.assertTrue(allTypes.doubleField == 3.21d);
        Assert.assertTrue((Boolean)booleanField.get(allTypes) == true);
        Assert.assertTrue((Byte)byteField.get(allTypes) == 123);
        Assert.assertTrue((Short)shortField.get(allTypes) == 12345);
        Assert.assertTrue((Integer)intField.get(allTypes) == 123456789);
        Assert.assertTrue((Long)longField.get(allTypes) == 123456789000L);
        Assert.assertTrue((Character)charField.get(allTypes) == 'x');
        Assert.assertTrue((Float)floatField.get(allTypes) == 1.23f);
        Assert.assertTrue((Double)doubleField.get(allTypes) == 3.21d);
        final Field stringField = name2field.get("stringField");
        stringField.set(allTypes, "xyz");
        Assert.assertEquals("xyz", allTypes.stringField);
        Assert.assertEquals("xyz", stringField.get(allTypes));
        final Field objectListField = name2field.get("objectListField");
        objectListField.set(allTypes, Arrays.asList("xyz", 42, null));
        Assert.assertTrue(Arrays.asList("xyz", 42, null).equals(allTypes.objectListField));
        Assert.assertTrue(Arrays.asList("xyz", 42, null).equals(objectListField.get(allTypes)));
    }

}
