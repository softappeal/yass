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
    @Override public void uncaughtException(final Thread t, final Throwable e) {
      System.err.println("### " + new Date() + " - " + ((t == null) ? "<null>" : t.getName()) + " - " + Exceptions.class.getName() + ':');
      if (e == null) {
        System.err.println("throwable is null");
      } else {
        e.printStackTrace();
        if (!(e instanceof Exception)) {
          System.exit(1);
        }
      }
    }
  };

}
