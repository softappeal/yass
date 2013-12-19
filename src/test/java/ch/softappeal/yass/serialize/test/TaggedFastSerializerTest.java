package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.SlowReflector;
import ch.softappeal.yass.serialize.TypeConverter;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.contract.Color;
import ch.softappeal.yass.serialize.contract.V1;
import ch.softappeal.yass.serialize.contract.V2;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeConverterId;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

public class TaggedFastSerializerTest {

  @Test public void notEnumeration() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(String.class),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type 'java.lang.String' is not an enumeration", e.getMessage());
    }
  }

  @Test public void duplicatedClass() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(Color.class, Color.class),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type 'ch.softappeal.yass.serialize.contract.Color' already added", e.getMessage());
    }
  }

  @Test public void abstractClass() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(Serializer.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type 'ch.softappeal.yass.serialize.Serializer' is abstract", e.getMessage());
    }
  }

  @Test public void illegalInterface() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(AutoCloseable.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type 'java.lang.AutoCloseable' is abstract", e.getMessage());
    }
  }

  @Test public void illegalAnnotation() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(Nullable.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type 'ch.softappeal.yass.util.Nullable' is abstract", e.getMessage());
    }
  }

  @Test public void illegalEnumeration() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(Color.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type 'ch.softappeal.yass.serialize.contract.Color' is an enumeration", e.getMessage());
    }
  }

  @Test public void missingType() throws Exception {
    final Serializer serializer = new TaggedFastSerializer(
      SlowReflector.FACTORY,
      Arrays.<TypeConverterId>asList(),
      Arrays.<Class<?>>asList(),
      Arrays.<Class<?>>asList(),
      Arrays.<Class<?>>asList()
    );
    try {
      JavaSerializerTest.copy(serializer, Color.BLUE);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("missing type 'ch.softappeal.yass.serialize.contract.Color'", e.getMessage());
    }
  }

  @Test public void printNumbers() {
    TestUtils.compareFile("ch/softappeal/yass/serialize/test/TaggedFastSerializerTest.numbers.txt", new TestUtils.Printer() {
      @Override public void print(final PrintWriter printer) {
        SerializerTest.TAGGED_FAST_SERIALIZER.printIds(printer);
      }
    });
  }

  @Test public void bytes() throws Exception {
    TestUtils.compareFile("ch/softappeal/yass/serialize/test/TaggedFastSerializerTest.bytes.txt", new TestUtils.Printer() {
      void write(final PrintWriter printer, final Object value) throws Exception {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        SerializerTest.TAGGED_FAST_SERIALIZER.write(value, Writer.create(buffer));
        final byte[] bytes = buffer.toByteArray();
        for (final byte b : bytes) {
          printer.print(" " + b);
        }
        printer.println();
      }
      @Override public void print(final PrintWriter printer) throws Exception {
        write(printer, SerializerTest.createGraph());
        write(printer, SerializerTest.createNulls());
        write(printer, SerializerTest.createValues());
      }
    });
  }

  private static final Serializer V1_SERIALIZER = new TaggedFastSerializer(
    FastReflector.FACTORY, Arrays.<TypeConverterId>asList(), Arrays.<Class<?>>asList(), Arrays.<Class<?>>asList(V1.class), Arrays.<Class<?>>asList()
  );
  private static final Serializer V2_SERIALIZER = new TaggedFastSerializer(
    FastReflector.FACTORY, Arrays.<TypeConverterId>asList(), Arrays.<Class<?>>asList(), Arrays.<Class<?>>asList(V2.class), Arrays.<Class<?>>asList()
  );

  private static V2 copy(final V1 v1) throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final Writer writer = Writer.create(buffer);
    V1_SERIALIZER.write(v1, writer);
    writer.writeByte((byte)123); // write sentinel
    final Reader reader = Reader.create(new ByteArrayInputStream(buffer.toByteArray()));
    final V2 v2 = (V2)V2_SERIALIZER.read(reader);
    Assert.assertTrue(reader.readByte() == 123); // check sentinel
    return v2;
  }

  @Test public void versioning() throws Exception {
    final V2 v2 = copy(new V1(42));
    Assert.assertTrue(v2.i1 == 42);
    Assert.assertNull(v2.i2);
    Assert.assertTrue(v2.i2() == 13);
  }

  @Test public void missingClassTag() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(String.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("missing tag for 'class java.lang.String'", e.getMessage());
    }
  }

  @Test public void duplicatedTypeTag() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(V1.class, V2.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertTrue(
        "type tag '100' used for 'ch.softappeal.yass.serialize.contract.V1' and 'ch.softappeal.yass.serialize.contract.V2'".equals(e.getMessage()) ||
        "type tag '100' used for 'ch.softappeal.yass.serialize.contract.V2' and 'ch.softappeal.yass.serialize.contract.V1'".equals(e.getMessage())
      );
    }
  }

  @Tag(-1) public static class InvalidTypeTag {
    // empty
  }

  @Test public void invalidTypeTag() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(InvalidTypeTag.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("tag '-1' for 'class ch.softappeal.yass.serialize.test.TaggedFastSerializerTest$InvalidTypeTag' must be >= 0", e.getMessage());
    }
  }

  @Tag(0) public static class DuplicatedFieldTag {
    @Tag(1) int i1;
    @Tag(1) int i2;
  }

  @Test public void duplicatedFieldTag() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeConverterId>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(DuplicatedFieldTag.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertTrue(
        "field tag '1' used for 'int ch.softappeal.yass.serialize.test.TaggedFastSerializerTest$DuplicatedFieldTag.i1' and 'int ch.softappeal.yass.serialize.test.TaggedFastSerializerTest$DuplicatedFieldTag.i2'".equals(e.getMessage()) ||
        "field tag '1' used for 'int ch.softappeal.yass.serialize.test.TaggedFastSerializerTest$DuplicatedFieldTag.i2' and 'int ch.softappeal.yass.serialize.test.TaggedFastSerializerTest$DuplicatedFieldTag.i1'".equals(e.getMessage())
      );
    }
  }

  @Test public void missingSerializableType() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeConverterId>asList(new TypeConverterId(
          new TypeConverter<BigInteger, BigDecimal>(BigInteger.class, BigDecimal.class) {
            @Override public BigDecimal to(final BigInteger value) {
              return null;
            }
            @Override public BigInteger from(final BigDecimal value) {
              return null;
            }
          },
          0
        )),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type converter 'java.math.BigInteger': no serializable type 'java.math.BigDecimal'", e.getMessage());
    }
  }

  @Test public void illegalSerializableType() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeConverterId>asList(new TypeConverterId(
          new TypeConverter<BigInteger, Void>(BigInteger.class, Void.class) {
            @Override public Void to(final BigInteger value) {
              return null;
            }
            @Override public BigInteger from(final Void value) {
              return null;
            }
          },
          0
        )),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type converter 'java.math.BigInteger': illegal serializable type 'java.lang.Void'", e.getMessage());
    }
  }

}
