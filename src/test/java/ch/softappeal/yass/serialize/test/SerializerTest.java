package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.contract.Binary;
import ch.softappeal.yass.serialize.contract.Color;
import ch.softappeal.yass.serialize.contract.IntException;
import ch.softappeal.yass.serialize.contract.LongClass;
import ch.softappeal.yass.serialize.contract.Node;
import ch.softappeal.yass.serialize.contract.PrimitiveTypes;
import ch.softappeal.yass.serialize.contract.nested.AllTypes;
import ch.softappeal.yass.serialize.convert.StringTypeConverter;
import ch.softappeal.yass.serialize.convert.TypeConverter;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeConverterId;
import ch.softappeal.yass.serialize.reflect.FastReflector;
import ch.softappeal.yass.serialize.reflect.SlowReflector;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SerializerTest {

  public static AllTypes createNulls() {
    return new AllTypes();
  }

  private static void checkNulls(final AllTypes allTypes) {
    Assert.assertTrue(allTypes.booleanField == false);
    Assert.assertTrue(allTypes.byteField == 0);
    Assert.assertTrue(allTypes.shortField == 0);
    Assert.assertTrue(allTypes.intField == 0);
    Assert.assertTrue(allTypes.longField == 0);
    Assert.assertTrue(allTypes.charField == 0);
    Assert.assertTrue(allTypes.floatField == 0);
    Assert.assertTrue(allTypes.doubleField == 0);
    Assert.assertNull(allTypes.booleanArrayField);
    Assert.assertNull(allTypes.byteArrayField);
    Assert.assertNull(allTypes.shortArrayField);
    Assert.assertNull(allTypes.intArrayField);
    Assert.assertNull(allTypes.longArrayField);
    Assert.assertNull(allTypes.charArrayField);
    Assert.assertNull(allTypes.floatArrayField);
    Assert.assertNull(allTypes.doubleArrayField);
    Assert.assertNull(allTypes.booleanWrapperField);
    Assert.assertNull(allTypes.byteWrapperField);
    Assert.assertNull(allTypes.shortWrapperField);
    Assert.assertNull(allTypes.intWrapperField);
    Assert.assertNull(allTypes.longWrapperField);
    Assert.assertNull(allTypes.charWrapperField);
    Assert.assertNull(allTypes.floatWrapperField);
    Assert.assertNull(allTypes.doubleWrapperField);
    Assert.assertNull(allTypes.stringField);
    Assert.assertNull(allTypes.colorField);
    Assert.assertNull(allTypes.bigDecimalField);
    Assert.assertNull(allTypes.bigIntegerField);
    Assert.assertNull(allTypes.binaryField);
    Assert.assertNull(allTypes.longClassField);
    Assert.assertNull(allTypes.primitiveTypesField);
    Assert.assertNull(allTypes.primitiveTypesListField);
    Assert.assertNull(allTypes.objectField);
    Assert.assertNull(allTypes.objectListField);
    Assert.assertNull(allTypes.throwable);
  }

  public static AllTypes createValues() {
    final AllTypes allTypes = new AllTypes();
    allTypes.booleanField = false;
    allTypes.byteField = (byte)100;
    allTypes.shortField = (short)101;
    allTypes.intField = 102;
    allTypes.longField = 103L;
    allTypes.charField = 'x';
    allTypes.floatField = 1.23f;
    allTypes.doubleField = 3.21d;
    allTypes.booleanWrapperField = true;
    allTypes.byteWrapperField = (byte)-100;
    allTypes.shortWrapperField = (short)-101;
    allTypes.intWrapperField = -102;
    allTypes.longWrapperField = -103L;
    allTypes.charWrapperField = 'y';
    allTypes.floatWrapperField = -1.23f;
    allTypes.doubleWrapperField = -3.21d;
    allTypes.booleanArrayField = new boolean[] {false, true, false};
    allTypes.byteArrayField = new byte[] {(byte)1, (byte)-2};
    allTypes.shortArrayField = new short[] {(short)-1, (short)2};
    allTypes.intArrayField = new int[] {1, -2};
    allTypes.longArrayField = new long[] {-1L, 2L};
    allTypes.charArrayField = new char[] {'x', 'y'};
    allTypes.floatArrayField = new float[] {-1f, 2f};
    allTypes.doubleArrayField = new double[] {1d, -2d};
    allTypes.stringField = "999";
    allTypes.colorField = Color.BLUE;
    allTypes.bigDecimalField = new BigDecimal("98.7");
    allTypes.bigIntegerField = new BigInteger("987");
    allTypes.binaryField = new Binary(new byte[] {(byte)123, (byte)-99});
    allTypes.longClassField = new LongClass(1234567890999988888L);
    allTypes.primitiveTypesField = new AllTypes("hello");
    allTypes.primitiveTypesListField = Arrays.asList(new PrimitiveTypes(999), new AllTypes("world"), null);
    allTypes.objectField = "bad";
    allTypes.objectListField = Arrays.<Object>asList("good", null, 123);
    allTypes.throwable = new IntException(123);
    return allTypes;
  }

  @SuppressWarnings("unchecked")
  private static void checkValues(final AllTypes allTypes) {
    Assert.assertTrue(allTypes.booleanField == false);
    Assert.assertTrue(allTypes.byteField == 100);
    Assert.assertTrue(allTypes.shortField == 101);
    Assert.assertTrue(allTypes.intField == 102);
    Assert.assertTrue(allTypes.longField == 103);
    Assert.assertTrue(allTypes.charField == 'x');
    Assert.assertTrue(allTypes.floatField == 1.23f);
    Assert.assertTrue(allTypes.doubleField == 3.21d);
    Assert.assertTrue(allTypes.booleanWrapperField.equals(true));
    Assert.assertTrue(allTypes.byteWrapperField.equals((byte)-100));
    Assert.assertTrue(allTypes.shortWrapperField.equals((short)-101));
    Assert.assertTrue(allTypes.intWrapperField.equals(-102));
    Assert.assertTrue(allTypes.longWrapperField.equals(-103L));
    Assert.assertTrue(allTypes.charWrapperField.equals('y'));
    Assert.assertTrue(allTypes.floatWrapperField.equals(-1.23f));
    Assert.assertTrue(allTypes.doubleWrapperField.equals(-3.21d));
    Assert.assertTrue(Arrays.equals(allTypes.booleanArrayField, new boolean[] {false, true, false}));
    Assert.assertTrue(Arrays.equals(allTypes.byteArrayField, new byte[] {(byte)1, (byte)-2}));
    Assert.assertTrue(Arrays.equals(allTypes.shortArrayField, new short[] {(short)-1, (short)2}));
    Assert.assertTrue(Arrays.equals(allTypes.intArrayField, new int[] {1, -2}));
    Assert.assertTrue(Arrays.equals(allTypes.longArrayField, new long[] {-1L, 2L}));
    Assert.assertTrue(Arrays.equals(allTypes.charArrayField, new char[] {'x', 'y'}));
    Assert.assertTrue(Arrays.equals(allTypes.floatArrayField, new float[] {-1f, 2f}));
    Assert.assertTrue(Arrays.equals(allTypes.doubleArrayField, new double[] {1d, -2d}));
    Assert.assertEquals("999", allTypes.stringField);
    Assert.assertEquals(Color.BLUE, allTypes.colorField);
    Assert.assertEquals(new BigDecimal("98.7"), allTypes.bigDecimalField);
    Assert.assertEquals(new BigInteger("987"), allTypes.bigIntegerField);
    Assert.assertArrayEquals(new byte[] {(byte)123, (byte)-99}, allTypes.binaryField.value());
    Assert.assertTrue(1234567890999988888L == allTypes.longClassField.value);
    Assert.assertEquals("hello", ((AllTypes)allTypes.primitiveTypesField).stringField);
    final List<PrimitiveTypes> primitiveTypesListField = allTypes.primitiveTypesListField;
    Assert.assertTrue(primitiveTypesListField.size() == 3);
    Assert.assertEquals(999, primitiveTypesListField.get(0).intField);
    Assert.assertEquals("world", ((AllTypes)primitiveTypesListField.get(1)).stringField);
    Assert.assertNull(primitiveTypesListField.get(2));
    Assert.assertEquals("bad", allTypes.objectField);
    final List<Object> objectListField = allTypes.objectListField;
    Assert.assertTrue(objectListField.size() == 3);
    Assert.assertEquals("good", objectListField.get(0));
    Assert.assertNull(objectListField.get(1));
    Assert.assertEquals(123, objectListField.get(2));
    Assert.assertTrue(((IntException)allTypes.throwable).value == 123);
  }

  public static Node createGraph() {
    final Node n1 = new Node(1);
    final Node n2 = new Node(2);
    final Node n3 = new Node(3);
    n1.link = n2;
    n2.link = n3;
    n3.link = n2;
    return n1;
  }

  private static void checkGraph(final Node n1) {
    final Node n2 = n1.link;
    final Node n3 = n2.link;
    Assert.assertTrue(n1.id == 1);
    Assert.assertTrue(n2.id == 2);
    Assert.assertTrue(n3.id == 3);
    Assert.assertSame(n3.link, n2);
  }

  private static void assertArrayEquals(final boolean[] b1, final boolean[] b2) {
    if (b1.length != b2.length) {
      throw new AssertionError();
    }
    for (int b = 0; b < b1.length; b++) {
      if (b1[b] != b2[b]) {
        throw new AssertionError();
      }
    }
  }

  private static void checkBaseTypes(final Serializer serializer) throws Exception {
    Assert.assertNull(JavaSerializerTest.copy(serializer, null));
    Assert.assertEquals(false, JavaSerializerTest.copy(serializer, false));
    Assert.assertEquals(true, JavaSerializerTest.copy(serializer, true));
    Assert.assertEquals((Byte)(byte)123, JavaSerializerTest.copy(serializer, (byte)123));
    Assert.assertEquals((Short)(short)123, JavaSerializerTest.copy(serializer, (short)123));
    Assert.assertEquals((Integer)123, JavaSerializerTest.copy(serializer, 123));
    Assert.assertEquals((Long)123L, JavaSerializerTest.copy(serializer, 123L));
    Assert.assertEquals((Character)'x', JavaSerializerTest.copy(serializer, 'x'));
    Assert.assertEquals((Float)1.23f, JavaSerializerTest.copy(serializer, 1.23f));
    Assert.assertEquals((Double)1.23d, JavaSerializerTest.copy(serializer, 1.23d));
    Assert.assertEquals("123", JavaSerializerTest.copy(serializer, "123"));
    Assert.assertEquals(Color.RED, JavaSerializerTest.copy(serializer, Color.RED));
    Assert.assertEquals(new BigInteger("123"), JavaSerializerTest.copy(serializer, new BigInteger("123")));
    Assert.assertEquals(new BigDecimal("1.23"), JavaSerializerTest.copy(serializer, new BigDecimal("1.23")));
    Assert.assertArrayEquals(new byte[] {(byte)1, (byte)2}, JavaSerializerTest.copy(serializer, new Binary(new byte[] {(byte)1, (byte)2})).value());
    Assert.assertTrue(Arrays.equals(new boolean[] {true, false}, JavaSerializerTest.copy(serializer, new boolean[] {true, false})));
    Assert.assertArrayEquals(new byte[] {(byte)1, (byte)2}, JavaSerializerTest.copy(serializer, new byte[] {(byte)1, (byte)2}));
    Assert.assertArrayEquals(new short[] {(short)1, (short)2}, JavaSerializerTest.copy(serializer, new short[] {(short)1, (short)2}));
    Assert.assertArrayEquals(new int[] {1, 2}, JavaSerializerTest.copy(serializer, new int[] {1, 2}));
    Assert.assertArrayEquals(new long[] {1L, 2L}, JavaSerializerTest.copy(serializer, new long[] {1L, 2L}));
    Assert.assertArrayEquals(new char[] {'a', 'b'}, JavaSerializerTest.copy(serializer, new char[] {'a', 'b'}));
    Assert.assertArrayEquals(new float[] {1f, 2f}, JavaSerializerTest.copy(serializer, new float[] {1f, 2f}), 0f);
    Assert.assertArrayEquals(new double[] {1d, 2d}, JavaSerializerTest.copy(serializer, new double[] {1d, 2d}), 0d);
    Assert.assertEquals(new ArrayList<Object>(), JavaSerializerTest.copy(serializer, new ArrayList<Object>()));
    Assert.assertEquals(Arrays.asList(1, null, "2"), JavaSerializerTest.copy(serializer, Arrays.asList(1, null, "2")));
    Assert.assertEquals(new ArrayList<String>(), JavaSerializerTest.copy(serializer, new ArrayList<String>()));
    Assert.assertEquals(Arrays.<String>asList("1", null, "2"), JavaSerializerTest.copy(serializer, Arrays.<String>asList("1", null, "2")));
    Assert.assertTrue(JavaSerializerTest.copy(serializer, new AllTypes()).getClass() == AllTypes.class);
    Assert.assertTrue(JavaSerializerTest.copy(serializer, new PrimitiveTypes()).getClass() == PrimitiveTypes.class);
    Assert.assertTrue(JavaSerializerTest.copy(serializer, new IntException(123)).value == 123);
    final boolean[] booleans = new boolean[10000];
    Arrays.fill(booleans, true);
    assertArrayEquals(JavaSerializerTest.copy(serializer, booleans), booleans);
    final byte[] bytes = new byte[10000];
    Arrays.fill(bytes, (byte)123);
    Assert.assertArrayEquals(JavaSerializerTest.copy(serializer, bytes), bytes);
    final short[] shorts = new short[10000];
    Arrays.fill(shorts, (short)12345);
    Assert.assertArrayEquals(JavaSerializerTest.copy(serializer, shorts), shorts);
    final int[] ints = new int[10000];
    Arrays.fill(ints, 12345678);
    Assert.assertArrayEquals(JavaSerializerTest.copy(serializer, ints), ints);
    final long[] longs = new long[10000];
    Arrays.fill(longs,12345678901234585L);
    Assert.assertArrayEquals(JavaSerializerTest.copy(serializer, longs), longs);
    final char[] chars = new char[10000];
    Arrays.fill(chars, 'x');
    Assert.assertArrayEquals(JavaSerializerTest.copy(serializer, chars), chars);
    final float[] floats = new float[10000];
    Arrays.fill(floats, 123.987f);
    Assert.assertArrayEquals(JavaSerializerTest.copy(serializer, floats), floats, 0);
    final double[] doubles = new double[10000];
    Arrays.fill(doubles, 123.987d);
    Assert.assertArrayEquals(JavaSerializerTest.copy(serializer, doubles), doubles, 0);
  }

  private static void test(final Serializer serializer) throws Exception {
    checkBaseTypes(serializer);
    checkNulls(JavaSerializerTest.copy(serializer, createNulls()));
    checkValues(JavaSerializerTest.copy(serializer, createValues()));
    checkGraph(JavaSerializerTest.copy(serializer, createGraph()));
  }

  @Test public void java() throws Exception {
    test(JavaSerializer.INSTANCE);
  }

  private static final List<TypeConverter> TYPE_CONVERTERS = Arrays.asList(StringTypeConverter.BIG_INTEGER, StringTypeConverter.BIG_DECIMAL, Binary.TYPE_CONVERTER, LongClass.TYPE_CONVERTER);
  private static final List<Class<?>> ENUMERATIONS = Arrays.<Class<?>>asList(Color.class);
  private static final List<Class<?>> CONCRETE_CLASSES = Arrays.<Class<?>>asList(PrimitiveTypes.class, AllTypes.class, IntException.class);
  private static final List<Class<?>> REFERENCEABLE_CONCRETE_CLASSES = Arrays.<Class<?>>asList(Node.class);

  static final AbstractFastSerializer FAST_SIMPLE_FAST_SERIALIZER = new SimpleFastSerializer(FastReflector.FACTORY, TYPE_CONVERTERS, ENUMERATIONS, CONCRETE_CLASSES, REFERENCEABLE_CONCRETE_CLASSES);
  static final AbstractFastSerializer SLOW_SIMPLE_FAST_SERIALIZER = new SimpleFastSerializer(SlowReflector.FACTORY, TYPE_CONVERTERS, ENUMERATIONS, CONCRETE_CLASSES, REFERENCEABLE_CONCRETE_CLASSES);

  @Test public void fastSimpleFast() throws Exception {
    test(FAST_SIMPLE_FAST_SERIALIZER);
  }

  @Test public void slowSimpleFast() throws Exception {
    test(SLOW_SIMPLE_FAST_SERIALIZER);
  }

  static final AbstractFastSerializer TAGGED_FAST_SERIALIZER = new TaggedFastSerializer(
    FastReflector.FACTORY,
    Arrays.asList(
      new TypeConverterId(StringTypeConverter.BIG_INTEGER, 0),
      new TypeConverterId(StringTypeConverter.BIG_DECIMAL, 1),
      new TypeConverterId(Binary.TYPE_CONVERTER, 2),
      new TypeConverterId(LongClass.TYPE_CONVERTER, 3)
    ),
    ENUMERATIONS,
    CONCRETE_CLASSES,
    REFERENCEABLE_CONCRETE_CLASSES
  );

  @Test public void taggedFast() throws Exception {
    test(TAGGED_FAST_SERIALIZER);
  }

}
