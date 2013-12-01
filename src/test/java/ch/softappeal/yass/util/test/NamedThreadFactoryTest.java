package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NamedThreadFactoryTest {

  @Test public void test() throws Exception {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    executor.execute(new Runnable() {
      @Override public void run() {
        throw new RuntimeException("run");
      }
    });
    TimeUnit.MILLISECONDS.sleep(100L);
    executor.shutdown();
  }

}
