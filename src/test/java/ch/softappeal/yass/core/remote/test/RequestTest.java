package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.Request;
import org.junit.Assert;
import org.junit.Test;

public class RequestTest {

    @Test public void test() {
        final Object serviceId = "abc";
        final String methodId = "xyz";
        final Object[] arguments = new Object[0];
        final Request request = new Request(serviceId, methodId, arguments);
        Assert.assertSame(serviceId, request.serviceId);
        Assert.assertSame(methodId, request.methodId);
        Assert.assertSame(arguments, request.arguments);
    }

}
