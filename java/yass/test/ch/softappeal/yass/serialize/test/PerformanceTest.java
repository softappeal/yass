package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.Dumper;
import ch.softappeal.yass.Stopwatch;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.test.DumperTest;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;

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
        var i = ITERATIONS;
        final var stopwatch = new Stopwatch();
        while (i-- > 0) {
            serializer.write(data, writer);
        }
        stopwatch.stop();
        return stopwatch;
    }

    private static Stopwatch readTest(final Object data, final Serializer serializer) throws Exception {
        final var buffer = new ByteArrayOutputStream();
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
        final var reader = new MyReader();
        var i = ITERATIONS;
        final var stopwatch = new Stopwatch();
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
        final var counter = new Counter();
        serializer.write(data, counter);
        final var count = counter.count;
        // warmup JIT
        writeTest(data, serializer);
        readTest(data, serializer);
        final var writeStopwatch = writeTest(data, serializer);
        final var readStopwatch = readTest(data, serializer);
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

    @Test public void showData() {
        final Dumper dumper = new DumperTest.TestDumper(false, true, BigInteger.class, BigDecimal.class, Instant.class);
        System.out.println(dumper.dump(SerializerTest.createGraph()));
        System.out.println(dumper.dump(SerializerTest.createNulls()));
        System.out.println(dumper.dump(SerializerTest.createValues()));
    }

}
