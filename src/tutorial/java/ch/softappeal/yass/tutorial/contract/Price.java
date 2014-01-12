package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;

public final class Price {

  public final String instrumentId;
  public final int value;
  public final PriceType type;
  public final DateTime timestamp;

  public Price(final String instrumentId, final int value, final PriceType type, final DateTime timestamp) {
    this.instrumentId = Check.notNull(instrumentId);
    this.value = value;
    this.type = Check.notNull(type);
    this.timestamp = Check.notNull(timestamp);
  }

}
