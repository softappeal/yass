package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;
import org.junit.Assert;
import org.junit.Test;

@Tag(123) public class CheckTest {

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

  @Test public void hasTag() {
    Assert.assertTrue(Check.hasTag(CheckTest.class) == 123);
    try {
      Check.hasTag(Check.class);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      Assert.assertEquals("missing tag for 'class ch.softappeal.yass.util.Check'", e.getMessage());
    }
  }

}
