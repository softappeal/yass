package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.SlowReflector;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.contract.C1;
import ch.softappeal.yass.serialize.contract.C2;
import ch.softappeal.yass.serialize.contract.Color;
import ch.softappeal.yass.serialize.contract.E1;
import ch.softappeal.yass.serialize.contract.E2;
import ch.softappeal.yass.serialize.contract.PrimitiveTypes;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.serialize.fast.BaseTypeHandler;
import ch.softappeal.yass.serialize.fast.BaseTypeHandlers;
import ch.softappeal.yass.serialize.fast.TaggedFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

public class AbstractFastSerializerTest {

  @Test public void baseEnumeration() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeDesc>asList(new TypeDesc(1, new BaseTypeHandler<Color>(Color.class) {
          @Override public Color read(final Reader reader) {
            return null;
          }
          @Override public void write(final Color value, final Writer writer) {
            // empty
          }
        })),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("base type 'ch.softappeal.yass.serialize.contract.Color' is an enumeration", e.getMessage());
    }
  }

  @Test public void notEnumeration() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeDesc>asList(),
        Arrays.<Class<?>>asList(PrimitiveTypes.class),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type 'ch.softappeal.yass.serialize.contract.PrimitiveTypes' is not an enumeration", e.getMessage());
    }
  }

  @Test public void duplicatedClass() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeDesc>asList(),
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
        Arrays.<TypeDesc>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(AbstractFastSerializer.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("type 'ch.softappeal.yass.serialize.fast.AbstractFastSerializer' is abstract", e.getMessage());
    }
  }

  @Test public void illegalInterface() {
    try {
      new TaggedFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeDesc>asList(),
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
        Arrays.<TypeDesc>asList(),
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
        Arrays.<TypeDesc>asList(),
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
      Arrays.<TypeDesc>asList(),
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

  @Test public void taggedPrint() {
    TestUtils.compareFile("ch/softappeal/yass/serialize/test/TaggedFastSerializerTest.numbers.txt", new TestUtils.Printer() {
      @Override public void print(final PrintWriter printer) {
        SerializerTest.TAGGED_FAST_SERIALIZER.print(printer);
      }
    });
  }

  @Test public void simplePrints() {
    TestUtils.compareFile("ch/softappeal/yass/serialize/test/SimpleFastSerializerTest.numbers.txt", new TestUtils.Printer() {
      @Override public void print(final PrintWriter printer) {
        SerializerTest.SIMPLE_FAST_SERIALIZER.print(printer);
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
    FastReflector.FACTORY, Arrays.asList(new TypeDesc(3, BaseTypeHandlers.INTEGER)), Arrays.<Class<?>>asList(E1.class), Arrays.<Class<?>>asList(C1.class), Arrays.<Class<?>>asList()
  );
  private static final Serializer V2_SERIALIZER = new TaggedFastSerializer(
    FastReflector.FACTORY, Arrays.asList(new TypeDesc(3, BaseTypeHandlers.INTEGER)), Arrays.<Class<?>>asList(E2.class), Arrays.<Class<?>>asList(C2.class), Arrays.<Class<?>>asList()
  );

  private static Object copy(final Object input) throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final Writer writer = Writer.create(buffer);
    V1_SERIALIZER.write(input, writer);
    writer.writeByte((byte)123); // write sentinel
    final Reader reader = Reader.create(new ByteArrayInputStream(buffer.toByteArray()));
    final Object output = V2_SERIALIZER.read(reader);
    Assert.assertTrue(reader.readByte() == 123); // check sentinel
    return output;
  }

  @Test public void versioning() throws Exception {
    final C2 c2 = (C2)copy(new C1(42));
    Assert.assertTrue(c2.i1 == 42);
    Assert.assertNull(c2.i2);
    Assert.assertTrue(c2.i2() == 13);
    Assert.assertSame(copy(E1.c1), E2.c1);
    Assert.assertSame(copy(E1.c2), E2.c2);
  }

  public static class MissingClassTag {
    @Tag(1) int i;
  }

  @Test public void missingClassTag() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeDesc>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(MissingClassTag.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("missing tag for 'class ch.softappeal.yass.serialize.test.AbstractFastSerializerTest$MissingClassTag'", e.getMessage());
    }
  }

  @Test public void duplicatedTypeTag() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeDesc>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(C1.class, C2.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
  }

  @Tag(-1) public static class InvalidTypeTag {
    // empty
  }

  @Test public void invalidTypeTag() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeDesc>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(InvalidTypeTag.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("id -1 for type 'ch.softappeal.yass.serialize.test.AbstractFastSerializerTest.InvalidTypeTag' must be >= 0", e.getMessage());
    }
  }

  @Tag(0) public static class InvalidFieldTag {
    @Tag(0) int i;
  }

  @Test public void invalidFieldTag() {
    try {
      new TaggedFastSerializer(
        FastReflector.FACTORY,
        Arrays.<TypeDesc>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(InvalidFieldTag.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals(
        "id 0 for field 'int ch.softappeal.yass.serialize.test.AbstractFastSerializerTest$InvalidFieldTag.i' must be >= 1",
        e.getMessage()
      );
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
        Arrays.<TypeDesc>asList(),
        Arrays.<Class<?>>asList(),
        Arrays.<Class<?>>asList(DuplicatedFieldTag.class),
        Arrays.<Class<?>>asList()
      );
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
  }

}
