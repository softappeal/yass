package ch.softappeal.yass.tutorial.server;

import java.util.Set;

/**
 * Shows how to do session safe dependency injection.
 */
public interface PriceEngineContext {

  Set<String> subscribedInstrumentIds();

}
