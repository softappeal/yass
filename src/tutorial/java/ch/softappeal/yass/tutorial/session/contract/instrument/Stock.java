package ch.softappeal.yass.tutorial.session.contract.instrument;

import ch.softappeal.yass.tutorial.session.contract.Instrument;
import ch.softappeal.yass.util.Tag;

@Tag(10) public final class Stock extends Instrument {

  @Tag(10) public final boolean paysDividend;

  public Stock(final String id, final String name, final boolean paysDividend) {
    super(id, name);
    this.paysDividend = paysDividend;
  }

}
