package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.SlowReflector;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.serialize.contract.Color;
import ch.softappeal.yass.serialize.convert.TypeConverter;
import ch.softappeal.yass.serialize.fast.SimpleFastSerializer;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

public class SimpleFastSerializerTest {

  @Test public void notEnumeration() {
    try {
      new SimpleFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverter>asList(),
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
      new SimpleFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverter>asList(),
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
      new SimpleFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverter>asList(),
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
      new SimpleFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverter>asList(),
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
      new SimpleFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverter>asList(),
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
      new SimpleFastSerializer(
        SlowReflector.FACTORY,
        Arrays.<TypeConverter>asList(),
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
    final Serializer serializer = new SimpleFastSerializer(
      SlowReflector.FACTORY,
      Arrays.<TypeConverter>asList(),
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
    TestUtils.compareFile("ch/softappeal/yass/serialize/test/SimpleFastSerializerTest.numbers.txt", new TestUtils.Printer() {
      @Override public void print(final PrintWriter printer) {
        SerializerTest.FAST_SIMPLE_FAST_SERIALIZER.printNumbers(printer);
      }
    });
  }

  @Test public void bytes() throws Exception {
    TestUtils.compareFile("ch/softappeal/yass/serialize/test/SimpleFastSerializerTest.bytes.txt", new TestUtils.Printer() {
      void write(final PrintWriter printer, final Object value) throws Exception {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        SerializerTest.FAST_SIMPLE_FAST_SERIALIZER.write(value, Writer.create(buffer));
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

}
