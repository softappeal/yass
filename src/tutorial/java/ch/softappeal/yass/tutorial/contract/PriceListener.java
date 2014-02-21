package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.util.Tag;

import java.util.List;

public interface PriceListener {

  @Tag(1) @OneWay void newPrices(List<Price> prices);

}
