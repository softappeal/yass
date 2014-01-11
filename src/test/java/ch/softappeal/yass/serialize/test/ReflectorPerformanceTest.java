package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.FastReflector;
import ch.softappeal.yass.serialize.Reflector;
import ch.softappeal.yass.serialize.SlowReflector;
import ch.softappeal.yass.serialize.contract.nested.AllTypes;
import ch.softappeal.yass.util.PerformanceTask;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReflectorPerformanceTest {

  private static void test(final Reflector.Factory reflectorFactory) throws Exception {
    new PerformanceTask() {
      @Override protected void run(final int count) throws Exception {
        final Reflector reflector = reflectorFactory.create(AllTypes.class);
        final Map<String, Reflector.Accessor> name2accessor = ReflectorTest.name2accessor(reflectorFactory, AllTypes.class);
        final Reflector.Accessor intField = name2accessor.get("intField");
        final Reflector.Accessor stringField = name2accessor.get("stringField");
        int counter = count;
        while (counter-- > 0) {
          final AllTypes allTypes = (AllTypes)reflector.newInstance();
          intField.set(allTypes, 123);
          stringField.set(allTypes, "xyz");
          Assert.assertTrue((Integer)intField.get(allTypes) == 123);
          Assert.assertEquals("xyz", stringField.get(allTypes));
        }
      }
    }.run(1, TimeUnit.NANOSECONDS);
  }

  @Test public void fast() throws Exception {
    test(FastReflector.FACTORY);
  }

  @Test public void slow() throws Exception {
    test(SlowReflector.FACTORY);
  }

  @Test public void java() {
    new PerformanceTask() {
      @Override protected void run(final int count) {
        int counter = count;
        while (counter-- > 0) {
          final AllTypes allTypes = new AllTypes();
          allTypes.intField = 123;
          allTypes.stringField = "xyz";
          Assert.assertTrue(allTypes.intField == 123);
          Assert.assertEquals("xyz", allTypes.stringField);
        }
      }
    }.run(1, TimeUnit.NANOSECONDS);
  }

}
