package ch.softappeal.yass.serialize.test;

public class NoDefaultConstructor {

  static boolean CONSTRUCTOR_CALLED;

  public final int i;

  public NoDefaultConstructor(final int i) {
    CONSTRUCTOR_CALLED = true;
    this.i = i;
  }

}
