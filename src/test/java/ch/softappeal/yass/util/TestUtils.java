package ch.softappeal.yass.util;

import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class TestUtils {

  public interface Printer {
    void print(PrintWriter printer) throws Exception;
  }

  public static void compareFile(final String fileResourcePath, final Printer printer) {
    try {
      final PrintWriter writer = new PrintWriter(System.out);
      printer.print(writer);
      writer.flush();
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      try (PrintWriter out = new PrintWriter(new OutputStreamWriter(buffer))) {
        printer.print(out);
      }
      final byte[] outBuffer = buffer.toByteArray();
      try (InputStream in = new ClassLoaderResource(TestUtils.class.getClassLoader(), fileResourcePath).create()) {
        final byte[] inBuffer = new byte[outBuffer.length];
        Assert.assertTrue(in.read(inBuffer) == inBuffer.length);
        Assert.assertTrue(in.read() < 0);
        Assert.assertArrayEquals(inBuffer, outBuffer);
      }
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

  public static final UncaughtExceptionHandler TERMINATE = new UncaughtExceptionHandler() {
    @Override public void uncaughtException(final Thread t, final Throwable e) {
      Exceptions.STD_ERR.uncaughtException(t, e);
      //noinspection CallToSystemExit
      System.exit(1);
    }
  };

}
