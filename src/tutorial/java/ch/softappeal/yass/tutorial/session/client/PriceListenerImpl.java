package ch.softappeal.yass.tutorial.session.client;

import ch.softappeal.yass.tutorial.session.contract.Price;
import ch.softappeal.yass.tutorial.session.contract.PriceListener;
import ch.softappeal.yass.util.ContextLocator;
import ch.softappeal.yass.util.ContextService;
import ch.softappeal.yass.util.Dumper;

import java.util.Date;
import java.util.List;

public final class PriceListenerImpl extends ContextService<PriceListenerContext> implements PriceListener {

  public PriceListenerImpl(final ContextLocator<PriceListenerContext> locator) {
    super(locator);
  }

  @Override public void newPrices(final List<Price> prices) {
    final PriceListenerContext context = context();
    final StringBuilder s = new StringBuilder();
    s.append(new Date() + " - newPrices from ").append(context.hashCode()).append(Dumper.LINE_SEPARATOR);
    for (final Price price : prices) {
      s.append("  ").append(context.getInstrument(price.instrumentId).name).append(": ").append(price.value).append(Dumper.LINE_SEPARATOR);
    }
    System.out.println(s);
  }

}
