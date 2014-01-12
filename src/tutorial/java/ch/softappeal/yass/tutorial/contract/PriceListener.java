package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.OneWay;

import java.util.List;

public interface PriceListener {

  @OneWay void newPrices(List<Price> prices);

}
