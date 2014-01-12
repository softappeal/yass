package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;

public abstract class Instrument {

  public final String id;
  public final String name;

  protected Instrument(final String id, final String name) {
    this.id = Check.notNull(id);
    this.name = Check.notNull(name);
  }

}
