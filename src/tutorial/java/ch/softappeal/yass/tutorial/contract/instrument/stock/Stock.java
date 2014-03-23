package ch.softappeal.yass.tutorial.contract.instrument.stock;

import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.util.Nullable;

/**
 * Shows deep nesting.
 */
public final class Stock extends Instrument {

  @Nullable public final Boolean paysDividend;

  public Stock(final String id, final String name, final Boolean paysDividend) {
    super(id, name);
    this.paysDividend = paysDividend;
  }

}
