package ch.softappeal.yass.core.test;

import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.util.PerformanceTask;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class PerformanceTest extends InvokeTest {

  public static PerformanceTask task(final TestService testService) {
    return new PerformanceTask() {
      @Override protected void run(final int count) throws Exception {
        int counter = count;
        while (counter-- > 0) {
          if (testService.divide(12, 3) != 4) {
            throw new RuntimeException();
          }
        }
      }
    };
  }

  @Test public void direct() {
    task(new TestServiceImpl()).run(100000, TimeUnit.NANOSECONDS);
  }

  @Test public void proxy() {
    task(Interceptors.proxy(TestService.class, new TestServiceImpl())).run(10000, TimeUnit.NANOSECONDS);
  }

}
