package ch.softappeal.yass.core.remote.test;

import ch.softappeal.yass.core.test.InvokeTest;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class PerformanceTest extends InvokeTest {

  @Test public void test() {
    ch.softappeal.yass.core.test.PerformanceTest.task(
      ContractIdTest.ID.invoker(ServerTest.client).proxy()
    ).run(1_000, TimeUnit.NANOSECONDS);
  }

}
