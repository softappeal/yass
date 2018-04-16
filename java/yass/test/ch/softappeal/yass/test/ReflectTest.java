package ch.softappeal.yass.test;

import ch.softappeal.yass.Reflect;
import ch.softappeal.yass.serialize.contract.nested.AllTypes;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReflectTest {

    @Test public void noDefaultConstructor() {
        final var instance = (NoDefaultConstructor)Reflect.ALLOCATE.apply(NoDefaultConstructor.class).get();
        Assert.assertTrue(instance.i == 0);
    }

    private static Map<String, Field> name2field(final Class<?> type) {
        return Reflect.allFields(type).stream().collect(Collectors.toMap(Field::getName, Function.identity()));
    }

    @Test public void fieldModifiers() throws Exception {
        FieldModifiers.CONSTRUCTOR_CALLED = false;
        final var fieldModifiers = (FieldModifiers)Reflect.ALLOCATE.apply(FieldModifiers.class).get();
        Assert.assertFalse(FieldModifiers.CONSTRUCTOR_CALLED);
        final var name2field = name2field(FieldModifiers.class);
        final var privateField = name2field.get("privateField");
        final var privateFinalField = name2field.get("privateFinalField");
        final var publicField = name2field.get("publicField");
        final var publicFinalField = name2field.get("publicFinalField");
        Assert.assertTrue(fieldModifiers.publicFinalField == 0);
        Assert.assertTrue((Integer)publicFinalField.get(fieldModifiers) == 0);
        Assert.assertTrue((Integer)privateFinalField.get(fieldModifiers) == 0);
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
        final var allTypes = (AllTypes)Reflect.ALLOCATE.apply(AllTypes.class).get();
        final var name2field = name2field(AllTypes.class);
        final var booleanField = name2field.get("booleanField");
        final var byteField = name2field.get("byteField");
        final var shortField = name2field.get("shortField");
        final var intField = name2field.get("intField");
        final var longField = name2field.get("longField");
        final var charField = name2field.get("charField");
        final var floatField = name2field.get("floatField");
        final var doubleField = name2field.get("doubleField");
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
        final var stringField = name2field.get("stringField");
        stringField.set(allTypes, "xyz");
        Assert.assertEquals("xyz", allTypes.stringField);
        Assert.assertEquals("xyz", stringField.get(allTypes));
        final var objectListField = name2field.get("objectListField");
        objectListField.set(allTypes, Arrays.asList("xyz", 42, null));
        Assert.assertTrue(Arrays.asList("xyz", 42, null).equals(allTypes.objectListField));
        Assert.assertTrue(Arrays.asList("xyz", 42, null).equals(objectListField.get(allTypes)));
    }

}
