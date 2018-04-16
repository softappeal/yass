package ch.softappeal.yass.remote.test;

import ch.softappeal.yass.remote.ExceptionReply;
import ch.softappeal.yass.remote.ReplyVisibility;
import ch.softappeal.yass.remote.ValueReply;
import org.junit.Assert;
import org.junit.Test;

public class ReplyTest {

    @Test public void value() throws Exception {
        final var value = "xyz";
        final var reply = new ValueReply(value);
        Assert.assertSame(value, reply.value);
        Assert.assertSame(value, ReplyVisibility.process(reply));
    }

    @Test public void exception() {
        final var exception = new Exception();
        final var reply = new ExceptionReply(exception);
        Assert.assertSame(exception, reply.exception);
        try {
            ReplyVisibility.process(reply);
            Assert.fail();
        } catch (final Exception e) {
            Assert.assertSame(exception, e);
        }
    }

}
