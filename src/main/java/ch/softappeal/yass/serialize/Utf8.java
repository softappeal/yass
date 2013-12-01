package ch.softappeal.yass.serialize;

import java.nio.charset.StandardCharsets;

public final class Utf8 {

  private Utf8() {
    // disable
  }

  public static byte[] bytes(final String string) {
    return string.getBytes(StandardCharsets.UTF_8);
  }

  public static String string(final byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

}
