package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.tutorial.contract.Instrument;

public final class Stock extends Instrument {

  public final boolean paysDividend;

  public Stock(final String id, final String name, final boolean paysDividend) {
    super(id, name);
    this.paysDividend = paysDividend;
  }

}
