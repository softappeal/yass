package ch.softappeal.yass.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;

public final class Exceptions {

  private Exceptions() {
    // disable
  }

  public static RuntimeException wrap(final Exception e) {
    return (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(Check.notNull(e));
  }

  /**
   * Prints to {@link System#err} and EXITS if {@code !(e instanceof Exception)}.
   * <p/>
   * Note: For productive code, this handler should be replaced with one that uses your logging framework!
   */
  public static final UncaughtExceptionHandler STD_ERR = new UncaughtExceptionHandler() {
    @Override public void uncaughtException(final Thread thread, final Throwable throwable) {
      System.err.println(
        "### " + new Date() + " - " + ((thread == null) ? "<null>" : thread.getName()) + " - " + Exceptions.class.getName() + ':'
      );
      if (throwable == null) {
        System.err.println("throwable is null");
      } else {
        throwable.printStackTrace();
        if (!(throwable instanceof Exception)) {
          System.exit(1);
        }
      }
    }
  };

}
