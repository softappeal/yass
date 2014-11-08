package ch.softappeal.yass.util;

/**
 * Base class for classes that need a {@link ContextLocator} injected.
 * @see #context()
 */
public abstract class ContextService<C> {

    protected ContextService(final ContextLocator<C> locator) {
        this.locator = Check.notNull(locator);
    }

    private final ContextLocator<C> locator;

    /**
     * @return {@link ContextLocator#context()}
     */
    protected final C context() {
        return locator.context();
    }

}
