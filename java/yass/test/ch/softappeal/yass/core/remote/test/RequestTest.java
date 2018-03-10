package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.Request;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class RequestTest {

    @Test public void test() {
        final var serviceId = 123456;
        final var methodId = 4711;
        final var arguments = List.of();
        final var request = new Request(serviceId, methodId, arguments);
        Assert.assertEquals(serviceId, request.serviceId);
        Assert.assertEquals(methodId, request.methodId);
        Assert.assertSame(arguments, request.arguments);
    }

}
