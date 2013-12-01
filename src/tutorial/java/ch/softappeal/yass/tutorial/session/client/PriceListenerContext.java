package ch.softappeal.yass.tutorial.session.client;

import ch.softappeal.yass.tutorial.session.contract.Instrument;

/**
 * Shows how to do session safe dependency injection.
 */
public interface PriceListenerContext {

  Instrument getInstrument(String instrumentId);

}
