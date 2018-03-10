package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.ExceptionReply;
import ch.softappeal.yass.core.remote.ReplyVisibility;
import ch.softappeal.yass.core.remote.ValueReply;
import org.junit.Assert;
import org.junit.Test;

public class ReplyTest {

    @Test public void value() throws Exception {
        final String value = "xyz";
        final ValueReply reply = new ValueReply(value);
        Assert.assertSame(value, reply.value);
        Assert.assertSame(value, ReplyVisibility.process(reply));
    }

    @Test public void exception() {
        final Exception exception = new Exception();
        final ExceptionReply reply = new ExceptionReply(exception);
        Assert.assertSame(exception, reply.exception);
        try {
            ReplyVisibility.process(reply);
            Assert.fail();
        } catch (final Exception e) {
            Assert.assertSame(exception, e);
        }
    }

}
