package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

@Tag(40) public final class Trade {

  @Tag(1) public final Instrument instrument;
  @Tag(2) public final int amount;

  public Trade(final Instrument instrument, final int amount) {
    this.instrument = Check.notNull(instrument);
    this.amount = amount;
  }

}
