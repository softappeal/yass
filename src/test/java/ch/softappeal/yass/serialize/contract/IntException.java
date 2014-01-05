package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.util.Tag;

@Tag(42) public class IntException extends Exception {

  private static final long serialVersionUID = 1L;

  @Tag(1) public int value;

  public IntException(final int value) {
    this.value = value;
  }

  public IntException() {
    // empty
  }

}
