package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.serialize.SlowReflector;
import ch.softappeal.yass.serialize.contract.nested.AllTypes;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReflectorTest {

  @Test public void fieldInitFast() throws Exception {
    final FieldInit f = (FieldInit)FastReflector.FACTORY.create(FieldInit.class).newInstance();
    Assert.assertTrue(f.i == 0);
    Assert.assertNull(f.s);
  }

  @Test public void fieldInitSlow() throws Exception {
    final FieldInit f = (FieldInit)SlowReflector.FACTORY.create(FieldInit.class).newInstance();
    Assert.assertTrue(f.i == 123);
    Assert.assertEquals(f.s, "abc");
  }

  static Map<String, Reflector.Accessor> name2accessor(final Reflector.Factory reflectorFactory, Class<?> type) throws Exception {
    final Reflector reflector = reflectorFactory.create(type);
    final Map<String, Reflector.Accessor> name2accessor = new HashMap<String, Reflector.Accessor>();
    while (type != null) {
      for (final Field field : type.getDeclaredFields()) {
        final int modifiers = field.getModifiers();
        if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
          final Reflector.Accessor accessor = reflector.accessor(field);
          if (name2accessor.put(field.getName(), accessor) != null) {
            Assert.fail("field '" + field.getName() + "' already added");
          }
        }
      }
      type = type.getSuperclass();
    }
    return name2accessor;
  }

  private static void noDefaultConstructor(final Reflector.Factory reflectorFactory) throws Exception {
    final Reflector reflector = reflectorFactory.create(NoDefaultConstructor.class);
    NoDefaultConstructor.CONSTRUCTOR_CALLED = false;
    reflector.newInstance();
    Assert.assertFalse(NoDefaultConstructor.CONSTRUCTOR_CALLED);
    final NoDefaultConstructor noDefaultConstructor = new NoDefaultConstructor(123);
    Assert.assertTrue(NoDefaultConstructor.CONSTRUCTOR_CALLED);
    Assert.assertTrue(noDefaultConstructor.i == 123);
  }

  @Test public void noDefaultConstructorSlow() throws Exception {
    try {
      noDefaultConstructor(SlowReflector.FACTORY);
    } catch (final NoSuchMethodException e) {
      System.out.println(e);
    }
  }

  @Test public void noDefaultConstructorFast() throws Exception {
    noDefaultConstructor(FastReflector.FACTORY);
  }

  private static void fieldModifiers(final Reflector.Factory reflectorFactory) throws Exception {
    final Reflector reflector = reflectorFactory.create(FieldModifiers.class);
    NoDefaultConstructor.CONSTRUCTOR_CALLED = false;
    final FieldModifiers fieldModifiers = (FieldModifiers)reflector.newInstance();
    Assert.assertFalse(NoDefaultConstructor.CONSTRUCTOR_CALLED);
    final Map<String, Reflector.Accessor> name2accessor = name2accessor(reflectorFactory, FieldModifiers.class);
    final Reflector.Accessor privateField = name2accessor.get("privateField");
    final Reflector.Accessor privateFinalField = name2accessor.get("privateFinalField");
    final Reflector.Accessor publicField = name2accessor.get("publicField");
    final Reflector.Accessor publicFinalField = name2accessor.get("publicFinalField");
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

  @Test public void fieldModifiersSlow() throws Exception {
    fieldModifiers(SlowReflector.FACTORY);
  }

  @Test public void fieldModifiersFast() throws Exception {
    fieldModifiers(FastReflector.FACTORY);
  }

  private static void allTypes(final Reflector.Factory reflectorFactory) throws Exception {
    final Reflector reflector = reflectorFactory.create(AllTypes.class);
    final AllTypes allTypes = (AllTypes)reflector.newInstance();
    final Map<String, Reflector.Accessor> name2accessor = name2accessor(reflectorFactory, AllTypes.class);
    final Reflector.Accessor booleanField = name2accessor.get("booleanField");
    final Reflector.Accessor byteField = name2accessor.get("byteField");
    final Reflector.Accessor shortField = name2accessor.get("shortField");
    final Reflector.Accessor intField = name2accessor.get("intField");
    final Reflector.Accessor longField = name2accessor.get("longField");
    final Reflector.Accessor charField = name2accessor.get("charField");
    final Reflector.Accessor floatField = name2accessor.get("floatField");
    final Reflector.Accessor doubleField = name2accessor.get("doubleField");
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
    final Reflector.Accessor stringField = name2accessor.get("stringField");
    stringField.set(allTypes, "xyz");
    Assert.assertEquals("xyz", allTypes.stringField);
    Assert.assertEquals("xyz", stringField.get(allTypes));
    final Reflector.Accessor objectListField = name2accessor.get("objectListField");
    objectListField.set(allTypes, Arrays.asList("xyz", 42, null));
    Assert.assertTrue(Arrays.asList("xyz", 42, null).equals(allTypes.objectListField));
    Assert.assertTrue(Arrays.asList("xyz", 42, null).equals(objectListField.get(allTypes)));
  }

  @Test public void allTypesSlow() throws Exception {
    allTypes(SlowReflector.FACTORY);
  }

  @Test public void allTypesFast() throws Exception {
    allTypes(FastReflector.FACTORY);
  }

}
