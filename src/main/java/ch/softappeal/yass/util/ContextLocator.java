package ch.softappeal.yass.util;

/**
 * This interface comes in handy if you need to inject a context depending on a {@link Thread}.
 * @see ContextService
 */
@FunctionalInterface public interface ContextLocator<C> {

    C context();

}
