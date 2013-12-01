package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Check;
import org.junit.Assert;
import org.junit.Test;

public class CheckTest {

  @Test public void notNull() {
    final String s = "BlaBli";
    Assert.assertSame(Check.notNull(s), s);
    try {
      Check.notNull(null);
      Assert.fail();
    } catch (final NullPointerException e) {
      // empty
    }
  }

}
