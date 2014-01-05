package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.test.InvocationTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.util.Tag;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.lang.reflect.Method;

public class TaggedMethodMapperTest {

  @Test public void mapping() {
    final String methodId = "xyz";
    final MethodMapper.Mapping mapping = new MethodMapper.Mapping(InvocationTest.METHOD, methodId, false);
    Assert.assertSame(InvocationTest.METHOD, mapping.method);
    Assert.assertSame(methodId, mapping.id);
    Assert.assertFalse(mapping.oneWay);
  }

  private interface Overloading {
    @Tag(123) void test();
    @Tag(123) void test(String s);
  }

  @Test public void tagOverloading() {
    try {
      TaggedMethodMapper.FACTORY.create(Overloading.class);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
  }

  private interface MissingTag {
    void test();
  }

  @Test public void missingTag() {
    try {
      TaggedMethodMapper.FACTORY.create(MissingTag.class);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("missing tag for 'public abstract void ch.softappeal.yass.core.remote.test.TaggedMethodMapperTest$MissingTag.test()'", e.getMessage());
    }
  }

  @Test public void tagFactory() throws NoSuchMethodException {
    final MethodMapper mapper = new TaggedMethodMapper(InvokeTest.TestService.class);
    {
      final Method method = InvokeTest.TestService.class.getMethod("throwError");
      final MethodMapper.Mapping mapping = mapper.mapMethod(method);
      Assert.assertEquals(method, mapping.method);
      Assert.assertEquals(22, mapping.id);
      Assert.assertFalse(mapping.oneWay);
      Assert.assertFalse(mapper.mapMethod(method).oneWay);
    }
    {
      final Method method = InvokeTest.TestService.class.getMethod("oneWay", int.class);
      final MethodMapper.Mapping mapping = mapper.mapId(33);
      Assert.assertEquals(method, mapping.method);
      Assert.assertEquals(33, mapping.id);
      Assert.assertTrue(mapping.oneWay);
      Assert.assertTrue(mapper.mapMethod(method).oneWay);
    }
    Assert.assertNull(mapper.mapId(999));
  }

  @Test public void printNumbers() {
    TestUtils.compareFile("ch/softappeal/yass/core/remote/test/TaggedMethodMapperTest.numbers.txt", new TestUtils.Printer() {
      @Override public void print(final PrintWriter printer) {
        new TaggedMethodMapper(InvokeTest.TestService.class).print(printer);
      }
    });
  }

}
