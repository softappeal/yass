package ch.softappeal.yass.autoconfig;

import ch.softappeal.yass.core.Interceptor;
import org.junit.Assert;

public final class ClassCollectorTest {

  public static void main(final String... args) throws Exception {
    new ClassCollector(Interceptor.class).classes.forEach(c -> System.out.println(c.getName()));
    System.out.println();
    new ClassCollector(Assert.class).classes.forEach(c -> System.out.println(c.getName()));
  }

}
