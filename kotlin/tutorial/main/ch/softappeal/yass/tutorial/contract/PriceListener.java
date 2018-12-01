package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.remote.*;

import java.util.*;

public interface PriceListener {

    @OneWay
    void newPrices(List<Price> prices);

}
