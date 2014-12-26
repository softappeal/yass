package ch.softappeal.yass.util.test;

import ch.softappeal.yass.serialize.test.SerializerTest;
import ch.softappeal.yass.util.Dumper;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Test;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class DumperTest {

    @Test public void test() {
        TestUtils.compareFile("ch/softappeal/yass/util/test/DumperTest.dump.txt", new TestUtils.Printer() {
            void dump(final Dumper dumper, final StringBuilder s, @Nullable final Object value) {
                s.append(dumper.toString(value)).append(Dumper.LINE_SEPARATOR);
            }
            void print(final Dumper dumper, final PrintWriter printer, final boolean cycles) {
                final StringBuilder s = new StringBuilder(1024);
                dump(dumper, s, null);
                dump(dumper, s, '\u001F');
                dump(dumper, s, '\u0020');
                dump(dumper, s, '\u007E');
                dump(dumper, s, '\u007F');
                dump(dumper, s, SerializerTest.createNulls());
                dump(dumper, s, SerializerTest.createValues());
                if (cycles) {
                    dump(dumper, s, SerializerTest.createGraph());
                }
                dump(dumper, s, Arrays.asList("one", "two", "three").toArray());
                final Map<Integer, String> int2string = new LinkedHashMap<>();
                int2string.put(1, "one");
                int2string.put(2, null);
                int2string.put(3, "three");
                dump(dumper, s, int2string);
                printer.append(s);
            }
            @Override public void print(final PrintWriter printer) {
                print(new Dumper(BigInteger.class, BigDecimal.class, Instant.class), printer, true);
                print(new Dumper(false, false), printer, false);
                print(new Dumper(true, true, BigInteger.class, BigDecimal.class, Instant.class), printer, true);
                print(new Dumper(true, false), printer, false);
            }
        });
    }

}
