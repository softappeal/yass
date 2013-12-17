package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.util.Stopwatch;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

public class PerformanceTest {

  private static final int ITERATIONS = 1;

  private static Stopwatch writeTest(final Object data, final Serializer serializer) throws Exception {
    final Writer writer = new Writer() {
      @Override public void writeByte(final byte value) throws Exception {
        // empty
      }
      @Override public void writeBytes(final byte[] buffer, final int offset, final int length) {
        // empty
      }
    };
    int i = ITERATIONS;
    final Stopwatch stopwatch = new Stopwatch();
    while (i-- > 0) {
      serializer.write(data, writer);
    }
    stopwatch.stop();
    return stopwatch;
  }

  private static Stopwatch readTest(final Object data, final Serializer serializer) throws Exception {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    serializer.write(data, Writer.create(buffer));
    buffer.close();
    class MyReader extends Reader {
      int index;
      private final byte[] bytes = buffer.toByteArray();
      @Override public byte readByte() throws Exception {
        return bytes[index++];
      }
      @Override public void readBytes(final byte[] buffer, final int offset, final int length) throws Exception {
        System.arraycopy(bytes, index, buffer, offset, length);
        index += length;
      }
    }
    final MyReader reader = new MyReader();
    int i = ITERATIONS;
    final Stopwatch stopwatch = new Stopwatch();
    while (i-- > 0) {
      reader.index = 0;
      serializer.read(reader);
    }
    stopwatch.stop();
    return stopwatch;
  }

  private static void test(final String name, final String type, final Object data, final Serializer serializer) throws Exception {
    class Counter extends Writer {
      private int count = 0;
      @Override public void writeByte(final byte b) {
        count++;
      }
      @Override public void writeBytes(final byte[] b, final int off, final int len) {
        count += len;
      }
    }
    final Counter counter = new Counter();
    serializer.write(data, counter);
    final int count = counter.count;
    // warmup JIT
    writeTest(data, serializer);
    readTest(data, serializer);
    final Stopwatch writeStopwatch = writeTest(data, serializer);
    final Stopwatch readStopwatch = readTest(data, serializer);
    System.out.printf("%s - %s - ", name, type);
    System.out.printf("bytes: %5d", count);
    System.out.printf(" write: %6.2fus", (writeStopwatch.microSeconds() / (double)ITERATIONS));
    System.out.printf(" read : %7.2fus", (readStopwatch.microSeconds() / (double)ITERATIONS));
    System.out.printf(" total: %7.2fus", ((writeStopwatch.microSeconds() + readStopwatch.microSeconds()) / (double)ITERATIONS));
    System.out.println();
  }

  private static void test(final String name, final Serializer serializer) throws Exception {
    test(name, "   graph", SerializerTest.createGraph(), serializer);
    test(name, "   nulls", SerializerTest.createNulls(), serializer);
    test(name, "  values", SerializerTest.createValues(), serializer);
    test(name, "emptyStr", "", serializer);
    test(name, " longStr", " 01234567890qaywsxedcrfvtgbzhnujmikolpPOLIKUJMZHNTGBRFVEDCWSXQAY", serializer);
    System.out.println();
  }

  @Test public void test() throws Exception {
    test("TaggedFast", SerializerTest.TAGGED_FAST_SERIALIZER);
    test("Java      ", JavaSerializer.INSTANCE);
  }

}
