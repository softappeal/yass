package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.util.Tag;

@Tag(30) public final class Stock extends Instrument {

  @Tag(10) public final boolean paysDividend;

  public Stock(final String id, final String name, final boolean paysDividend) {
    super(id, name);
    this.paysDividend = paysDividend;
  }

}
