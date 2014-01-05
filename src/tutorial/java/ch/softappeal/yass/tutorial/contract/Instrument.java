package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

public abstract class Instrument {

  @Tag(1) public final String id;
  @Tag(2) public final String name;

  protected Instrument(final String id, final String name) {
    this.id = Check.notNull(id);
    this.name = Check.notNull(name);
  }

}
