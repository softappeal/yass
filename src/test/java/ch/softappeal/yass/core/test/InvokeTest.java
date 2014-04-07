package ch.softappeal.yass.core.test;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;
import org.junit.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class InvokeTest {

  public static final class DivisionByZeroException extends Exception {

    private static final long serialVersionUID = 1L;

    public DivisionByZeroException(final int value) {
      this.value = value;
    }

    public final int value;

    @Override public String getMessage() {
      return value + " / 0";
    }

  }

  public interface TestService {

    @Tag(0) void nothing();

    @Tag(11) int divide(int a, int b) throws DivisionByZeroException;

    @Tag(22) void throwError();

    @Tag(33) @OneWay void oneWay(int sleepMillis);

    @Tag(34) void delay(int milliSeconds);

  }

  public static void println(final String name, final String type, final Object message) {
    System.out.printf(
      "%10s | %15s | %9s | %20s | %s\n",
      System.nanoTime() / 1000000L, name, type, Thread.currentThread().getName(), message
    );
  }

  public static final class TestServiceImpl implements TestService {

    @Override public void nothing() {
      println("impl", "", "nothing");
    }

    @Override public int divide(final int a, final int b) throws DivisionByZeroException {
      if (b == 0) {
        throw new DivisionByZeroException(a);
      }
      return (a / b);
    }

    @Override public void throwError() {
      throw new Error("throwError");
    }

    @Override public void oneWay(final int sleepMillis) {
      if (sleepMillis < 0) {
        return;
      }
      println("impl", "", "oneWay(" + sleepMillis + ')');
      try {
        TimeUnit.MILLISECONDS.sleep(sleepMillis);
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    @Override public void delay(final int milliSeconds) {
      try {
        TimeUnit.MILLISECONDS.sleep(milliSeconds);
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

  }

  public static final class Logger implements Interceptor {

    private final String name;

    public Logger(final String name) {
      this.name = Check.notNull(name);
    }

    @Override public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
      println(name, "entry", method.getName() + ' ' + Arrays.deepToString(arguments));
      try {
        final Object result = invocation.proceed();
        println(name, "exit", result);
        return result;
      } catch (final Throwable t) {
        println(name, "exception", t);
        throw t;
      }
    }

  }

  private static final AtomicInteger COUNTER = new AtomicInteger();
  private static final AtomicReference<Method> METHOD = new AtomicReference<>();
  private static final AtomicReference<Object[]> ARGUMENTS = new AtomicReference<>();

  public static boolean isOneWay(final String method) {
    return "oneWay".equals(method);
  }

  public static final Interceptor PRINTLN_AFTER = (method, arguments, invocation) -> {
    try {
      return invocation.proceed();
    } finally {
      System.out.println();
    }
  };

  private static void checkArguments(@Nullable final Object[] arguments) {
    if (ARGUMENTS.get() == null) {
      Assert.assertTrue((arguments == null) || (arguments.length == 0));
    } else {
      Assert.assertArrayEquals(ARGUMENTS.get(), arguments);
    }
  }

  public static final Interceptor CLIENT_INTERCEPTOR = Interceptor.composite(
    (method, arguments, invocation) -> {
      Assert.assertTrue(COUNTER.incrementAndGet() == 1);
      Assert.assertEquals(METHOD.get(), method);
      checkArguments(arguments);
      return invocation.proceed();
    },
    new Logger("client")
  );

  public static final Interceptor SERVER_INTERCEPTOR = Interceptor.composite(
    (method, arguments, invocation) -> {
      Assert.assertTrue(COUNTER.incrementAndGet() == 2);
      Assert.assertEquals(METHOD.get(), method);
      checkArguments(arguments);
      return invocation.proceed();
    },
    new Logger("server")
  );

  private static void invoke(final String method, final Class<?>[] types, @Nullable final Object[] arguments, final Runnable runnable) {
    try {
      METHOD.set(TestService.class.getMethod(method, types));
    } catch (final NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    ARGUMENTS.set(arguments);
    COUNTER.set(0);
    runnable.run();
    if (isOneWay(METHOD.get().getName())) {
      Assert.assertTrue(COUNTER.intValue() >= 1);
    } else {
      Assert.assertTrue(COUNTER.intValue() == 2);
    }
  }

  /**
   * @param testService must use {@link #CLIENT_INTERCEPTOR} and {@link #SERVER_INTERCEPTOR}
   */
  public static void invoke(final TestService testService) throws InterruptedException {
    invoke("nothing", new Class<?>[] {}, null, () -> {
      testService.nothing();
    });
    invoke("divide", new Class<?>[] {int.class, int.class}, new Object[] {12, 3}, () -> {
      try {
        Assert.assertTrue(testService.divide(12, 3) == 4);
      } catch (final DivisionByZeroException e) {
        Assert.fail();
      }
    });
    invoke("divide", new Class<?>[] {int.class, int.class}, new Object[] {123, 0}, () -> {
      try {
        testService.divide(123, 0);
        Assert.fail();
      } catch (final DivisionByZeroException e) {
        Assert.assertTrue(e.value == 123);
      }
    });
    invoke("throwError", new Class<?>[] {}, null, () -> {
      try {
        testService.throwError();
        Assert.fail();
      } catch (final Error e) {
        Assert.assertEquals("throwError", e.getMessage());
      }
    });
    invoke("oneWay", new Class<?>[] {int.class}, new Object[] {100}, () -> {
      testService.oneWay(100);
    });
    TimeUnit.MILLISECONDS.sleep(200L);
  }

}
