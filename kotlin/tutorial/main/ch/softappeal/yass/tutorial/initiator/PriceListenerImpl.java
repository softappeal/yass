package ch.softappeal.yass.tutorial.initiator;

import ch.softappeal.yass.tutorial.contract.*;

import java.util.*;

public final class PriceListenerImpl implements PriceListener {

    private PriceListenerImpl() {
        // disable
    }

    @Override
    public void newPrices(final List<Price> prices) {
        // do something with prices
    }

    public static final PriceListener INSTANCE = new PriceListenerImpl();

}
