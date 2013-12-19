package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

public abstract class Instrument {

  @Tag(0) public final String id;
  @Tag(1) public final String name;

  protected Instrument(final String id, final String name) {
    this.id = Check.notNull(id);
    this.name = Check.notNull(name);
  }

}
