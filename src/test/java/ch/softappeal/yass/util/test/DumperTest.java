package ch.softappeal.yass.util.test;

import ch.softappeal.yass.serialize.test.SerializerTest;
import ch.softappeal.yass.util.Dumper;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Test;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class DumperTest {

  @Test public void test() {
    final Dumper dumper = new Dumper(BigInteger.class, BigDecimal.class);
    TestUtils.compareFile("ch/softappeal/yass/util/test/DumperTest.dump.txt", new TestUtils.Printer() {
      void dump(final StringBuilder s, @Nullable final Object value) {
        s.append(dumper.toString(value)).append(Dumper.LINE_SEPARATOR);
      }
      @Override public void print(final PrintWriter printer) {
        final StringBuilder s = new StringBuilder(1024);
        dump(s, null);
        dump(s, '\u001F');
        dump(s, '\u0020');
        dump(s, '\u007E');
        dump(s, '\u007F');
        dump(s, SerializerTest.createNulls());
        dump(s, SerializerTest.createValues());
        dump(s, SerializerTest.createGraph());
        dump(s, Arrays.asList("one", "two", "three").toArray());
        final Map<Integer, String> int2string = new LinkedHashMap<>();
        int2string.put(1, "one");
        int2string.put(2, null);
        int2string.put(3, "three");
        dump(s, int2string);
        printer.append(s);
      }
    });
  }

}
