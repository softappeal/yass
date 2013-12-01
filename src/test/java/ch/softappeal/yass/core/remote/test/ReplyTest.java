package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.remote.ExceptionReply;
import ch.softappeal.yass.core.remote.ReplyVisibility;
import ch.softappeal.yass.core.remote.ValueReply;
import org.junit.Assert;
import org.junit.Test;

public class ReplyTest {

  @Test public void value() throws Throwable {
    final Object context = new Object();
    final String value = "xyz";
    final ValueReply reply = new ValueReply(context, value);
    Assert.assertSame(context, reply.context);
    Assert.assertSame(value, reply.value);
    Assert.assertSame(value, ReplyVisibility.process(reply));
  }

  @Test public void exception() {
    final Object context = new Object();
    final Throwable throwable = new Throwable();
    final ExceptionReply reply = new ExceptionReply(context, throwable);
    Assert.assertSame(context, reply.context);
    Assert.assertSame(throwable, reply.throwable);
    try {
      ReplyVisibility.process(reply);
      Assert.fail();
    } catch (final Throwable t) {
      Assert.assertSame(throwable, t);
    }
  }

}
