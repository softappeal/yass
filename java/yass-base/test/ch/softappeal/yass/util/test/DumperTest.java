package ch.softappeal.yass.util.test;

import ch.softappeal.yass.serialize.test.SerializerTest;
import ch.softappeal.yass.util.Dumper;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Ignore // $$$
public class DumperTest {

    private static final class TestDumper extends Dumper {
        TestDumper(final boolean compact, final boolean referenceables, final Class<?>... concreteValueClasses) {
            super(compact, referenceables, concreteValueClasses);
        }
        @Override protected boolean dumpValueClass(final StringBuilder out, final Class<?> type, final Object object) {
            if (isConcreteValueClass(type) || (Date.class == type)) {
                out.append(object);
                return true;
            }
            return false;
        }
    }

    @Test public void test() {
        TestUtils.compareFile("ch/softappeal/yass/util/test/DumperTest.dump.txt", new TestUtils.Printer() {
            void dump(final Dumper dumper, final StringBuilder s, final @Nullable Object value) {
                s.append(dumper.append(new StringBuilder(256), value)).append('\n');
            }
            void print(final Dumper dumper, final PrintWriter printer, final boolean cycles) {
                final StringBuilder s = new StringBuilder(1024);
                dump(dumper, s, null);
                dump(dumper, s, 'c');
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
                print(new TestDumper(false, true, BigInteger.class, BigDecimal.class, Instant.class), printer, true);
                print(new TestDumper(false, false), printer, false);
                print(new TestDumper(true, true, BigInteger.class, BigDecimal.class, Instant.class), printer, true);
                print(new TestDumper(true, false), printer, false);
            }
        });
    }

}
