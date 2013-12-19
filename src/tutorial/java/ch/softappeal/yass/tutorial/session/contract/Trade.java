package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

@Tag(20) public final class Trade {

  @Tag(0) public final Instrument instrument;
  @Tag(1) public final double amount;

  public Trade(final Instrument instrument, final double amount) {
    this.instrument = Check.notNull(instrument);
    this.amount = amount;
  }

}
