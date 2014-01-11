package ch.softappeal.yass.serialize;

import java.nio.charset.StandardCharsets;

public final class Utf8 {

  private Utf8() {
    // disable
  }

  public static byte[] bytes(final String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  public static String string(final byte[] value) {
    return new String(value, StandardCharsets.UTF_8);
  }

}
