package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

@Tag(22) public final class Price {

  @Tag(1) public final String instrumentId;
  @Tag(2) public final int value;
  @Tag(3) public final PriceType type;
  @Tag(7) public final DateTime timestamp;

  public Price(final String instrumentId, final int value, final PriceType type, final DateTime timestamp) {
    this.instrumentId = Check.notNull(instrumentId);
    this.value = value;
    this.type = Check.notNull(type);
    this.timestamp = Check.notNull(timestamp);
  }

}
