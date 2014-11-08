package ch.softappeal.yass.tutorial.client;

import ch.softappeal.yass.tutorial.contract.Instrument;

/**
 * Shows how to do session safe dependency injection.
 */
public interface PriceListenerContext {

    Instrument getInstrument(String instrumentId);

}
