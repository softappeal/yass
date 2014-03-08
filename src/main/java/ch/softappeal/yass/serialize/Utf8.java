package ch.softappeal.yass.serialize;

import java.nio.charset.Charset;

public final class Utf8 {

  private Utf8() {
    // disable
  }

  private static final Charset CHARSET = Charset.forName("UTF-8");

  public static byte[] bytes(final String string) {
    return string.getBytes(CHARSET);
  }

  public static String string(final byte[] bytes) {
    return new String(bytes, CHARSET);
  }

}
