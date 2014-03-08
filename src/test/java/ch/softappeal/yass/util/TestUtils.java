package ch.softappeal.yass.util;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.Charset;

public class TestUtils {

  public interface Printer {
    void print(PrintWriter printer) throws Exception;
  }

  public static void compareFile(final String fileResourcePath, final Printer printer) {
    try {
      final PrintWriter writer = new PrintWriter(System.out);
      printer.print(writer);
      writer.flush();
      final CharArrayWriter buffer = new CharArrayWriter();
      final PrintWriter out = new PrintWriter(buffer);
      try {
        printer.print(out);
      } finally {
        out.close();
      }
      final BufferedReader testReader = new BufferedReader(new CharArrayReader(buffer.toCharArray()));
      final BufferedReader refReader = new BufferedReader(new InputStreamReader(
        new ClassLoaderResource(TestUtils.class.getClassLoader(), fileResourcePath).create(),
        Charset.forName("UTF-8")
      ));
      try {
        while (true) {
          final String testLine = testReader.readLine();
          final String refLine = refReader.readLine();
          if ((testLine == null) && (refLine == null)) {
            return;
          }
          if ((testLine == null) || (refLine == null)) {
            throw new RuntimeException("files don't have same length");
          }
          Assert.assertEquals(testLine, refLine);
        }
      } finally {
        refReader.close();
      }
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

  public static final UncaughtExceptionHandler TERMINATE = new UncaughtExceptionHandler() {
    @Override public void uncaughtException(final Thread t, final Throwable e) {
      Exceptions.STD_ERR.uncaughtException(t, e);
      System.exit(1);
    }
  };

}
