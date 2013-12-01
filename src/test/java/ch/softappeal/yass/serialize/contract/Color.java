package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

@Tag(10) public enum Color {

  RED("red"),
  GREEN("green"),
  BLUE("blue");

  Color(final String text) {
    this.text = Check.notNull(text);
  }

  private final String text;

  public String getText() {
    return text;
  }

}
