package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

import java.math.BigDecimal;

@Tag(2) public final class Price {

  @Tag(0) public final String instrumentId;
  @Tag(1) public final BigDecimal value;
  @Tag(2) public final PriceType type;
  @Tag(7) public final DateTime timestamp;

  public Price(final String instrumentId, final BigDecimal value, final PriceType type, final DateTime timestamp) {
    this.instrumentId = Check.notNull(instrumentId);
    this.value = Check.notNull(value);
    this.type = Check.notNull(type);
    this.timestamp = Check.notNull(timestamp);
  }

}
