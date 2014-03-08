package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Stopwatch;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class StopwatchTest {

  @Test public void test() throws InterruptedException {
    final Stopwatch stopwatch = new Stopwatch();
    try {
      stopwatch.microSeconds();
      Assert.fail();
    } catch (final IllegalStateException e) {
      Assert.assertEquals("stopwatch is not yet stopped", e.getMessage());
    }
    TimeUnit.MILLISECONDS.sleep(50L);
    stopwatch.stop();
    try {
      stopwatch.stop();
      Assert.fail();
    } catch (final IllegalStateException e) {
      Assert.assertEquals("stopwatch is already stopped", e.getMessage());
    }
    Assert.assertTrue(stopwatch.seconds() == 0L);
    Assert.assertTrue(stopwatch.milliSeconds() >= 10L);
    Assert.assertTrue(stopwatch.microSeconds() >= 10000L);
    Assert.assertTrue(stopwatch.nanoSeconds() >= 10000000L);
  }

}
