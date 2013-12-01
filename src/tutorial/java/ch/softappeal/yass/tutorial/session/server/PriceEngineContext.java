package ch.softappeal.yass.tutorial.session.server;

import java.util.Set;

/**
 * Shows how to do session safe dependency injection.
 */
public interface PriceEngineContext {

  Set<String> subscribedInstrumentIds();

}
