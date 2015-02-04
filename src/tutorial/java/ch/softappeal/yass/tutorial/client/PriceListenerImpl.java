package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.tutorial.contract.Price;
import ch.softappeal.yass.tutorial.contract.PriceListener;

import java.util.List;

public final class PriceListenerImpl implements PriceListener {

    @Override public void newPrices(final List<Price> prices) {
        // do something with prices
    }

}
