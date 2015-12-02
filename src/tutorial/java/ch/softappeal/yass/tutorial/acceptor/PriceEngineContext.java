package ch.softappeal.yass.tutorial.acceptor;

import java.util.Set;

/**
 * Shows how to do session safe dependency injection.
 */
public interface PriceEngineContext {

    Set<Integer> subscribedInstrumentIds();

}
