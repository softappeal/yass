package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.test.InterceptorTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.util.Tag;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class MethodMapperTest {

  @Test public void mapping() {
    final String methodId = "xyz";
    final MethodMapper.Mapping mapping = new MethodMapper.Mapping(InterceptorTest.METHOD, methodId, false);
    Assert.assertSame(InterceptorTest.METHOD, mapping.method);
    Assert.assertSame(methodId, mapping.id);
    Assert.assertFalse(mapping.oneWay);
  }

  private interface TagOverloading {
    @Tag(123) void test();
    @Tag(123) void test(String s);
  }

  @Test public void tagOverloading() {
    try {
      TaggedMethodMapper.FACTORY.create(TagOverloading.class);
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
      Assert.assertEquals("missing tag for 'public abstract void ch.softappeal.yass.core.remote.test.MethodMapperTest$MissingTag.test()'", e.getMessage());
    }
  }

  @Test public void taggedFactory() throws NoSuchMethodException {
    final MethodMapper mapper = TaggedMethodMapper.FACTORY.create(InvokeTest.TestService.class);
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

  private interface OnewayResult {
    @Tag(1) @OneWay int test();
  }

  @Test public void onewayResult() {
    try {
      TaggedMethodMapper.FACTORY.create(OnewayResult.class);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("oneway method 'public abstract int ch.softappeal.yass.core.remote.test.MethodMapperTest$OnewayResult.test()' must return 'void'", e.getMessage());
    }
  }

  private interface OnewayException {
    @Tag(1) @OneWay void test() throws Exception;
  }

  @Test public void onewayException() {
    try {
      TaggedMethodMapper.FACTORY.create(OnewayException.class);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("oneway method 'public abstract void ch.softappeal.yass.core.remote.test.MethodMapperTest$OnewayException.test() throws java.lang.Exception' must not throw exceptions", e.getMessage());
    }
  }

  @Test public void simpleFactory() throws NoSuchMethodException {
    final MethodMapper mapper = SimpleMethodMapper.FACTORY.create(InvokeTest.TestService.class);
    {
      final Method method = InvokeTest.TestService.class.getMethod("throwError");
      final MethodMapper.Mapping mapping = mapper.mapMethod(method);
      Assert.assertEquals(method, mapping.method);
      Assert.assertEquals(4, mapping.id);
      Assert.assertFalse(mapping.oneWay);
      Assert.assertFalse(mapper.mapMethod(method).oneWay);
    }
    {
      final Method method = InvokeTest.TestService.class.getMethod("oneWay", int.class);
      final MethodMapper.Mapping mapping = mapper.mapId(3);
      Assert.assertEquals(method, mapping.method);
      Assert.assertEquals(3, mapping.id);
      Assert.assertTrue(mapping.oneWay);
      Assert.assertTrue(mapper.mapMethod(method).oneWay);
    }
  }

  private interface NameOverloading {
    void test();
    void test(String s);
  }

  @Test public void nameOverloading() {
    try {
      SimpleMethodMapper.FACTORY.create(NameOverloading.class);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      System.out.println(e.getMessage());
    }
  }

}
