package ch.softappeal.yass.remote.test;

import ch.softappeal.yass.Tag;
import ch.softappeal.yass.TestUtils;
import ch.softappeal.yass.remote.MethodMapper;
import ch.softappeal.yass.remote.OneWay;
import ch.softappeal.yass.remote.SimpleMethodMapper;
import ch.softappeal.yass.remote.TaggedMethodMapper;
import ch.softappeal.yass.test.InterceptorTest;
import ch.softappeal.yass.test.InvokeTest;
import org.junit.Assert;
import org.junit.Test;

public class MethodMapperTest {

    @Test public void mapping() {
        final var methodId = 1147;
        final var mapping = new MethodMapper.Mapping(InterceptorTest.METHOD, methodId, false);
        Assert.assertSame(InterceptorTest.METHOD, mapping.method);
        Assert.assertEquals(methodId, mapping.id);
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
            Assert.assertEquals(
                "missing tag for 'public abstract void ch.softappeal.yass.remote.test.MethodMapperTest$MissingTag.test()'",
                e.getMessage()
            );
        }
    }

    @Test public void taggedFactory() throws NoSuchMethodException {
        final var mapper = TaggedMethodMapper.FACTORY.create(InvokeTest.TestService.class);
        {
            final var method = InvokeTest.TestService.class.getMethod("nothing");
            final var mapping = mapper.mapMethod(method);
            Assert.assertEquals(method, mapping.method);
            Assert.assertEquals(0, mapping.id);
            Assert.assertFalse(mapping.oneWay);
            Assert.assertFalse(mapper.mapMethod(method).oneWay);
        }
        {
            final var method = InvokeTest.TestService.class.getMethod("oneWay", int.class);
            final var mapping = mapper.mapId(33);
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
            Assert.assertEquals(
                "oneWay method 'public abstract int ch.softappeal.yass.remote.test.MethodMapperTest$OnewayResult.test()' must 'return' void",
                e.getMessage()
            );
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
            Assert.assertEquals(
                "oneWay method 'public abstract void ch.softappeal.yass.remote.test.MethodMapperTest$OnewayException.test() throws java.lang.Exception' must not throw exceptions",
                e.getMessage()
            );
        }
    }

    @Test public void simpleFactory() throws NoSuchMethodException {
        final var mapper = SimpleMethodMapper.FACTORY.create(InvokeTest.TestService.class);
        {
            final var method = InvokeTest.TestService.class.getMethod("nothing");
            final var mapping = mapper.mapMethod(method);
            Assert.assertEquals(method, mapping.method);
            Assert.assertEquals(1, mapping.id);
            Assert.assertFalse(mapping.oneWay);
            Assert.assertFalse(mapper.mapMethod(method).oneWay);
        }
        {
            final var method = InvokeTest.TestService.class.getMethod("oneWay", int.class);
            final var mapping = mapper.mapId(2);
            Assert.assertEquals(method, mapping.method);
            Assert.assertEquals(2, mapping.id);
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

    @Test public void simplePrint() {
        TestUtils.compareFile(
            "ch/softappeal/yass/remote/test/SimpleMethodMapperTest.txt",
            printer -> MethodMapper.print(printer, SimpleMethodMapper.FACTORY, InvokeTest.TestService.class)
        );
    }

    @Test public void taggedPrint() {
        TestUtils.compareFile(
            "ch/softappeal/yass/remote/test/TaggedMethodMapperTest.txt",
            printer -> MethodMapper.print(printer, TaggedMethodMapper.FACTORY, InvokeTest.TestService.class)
        );
    }

}
