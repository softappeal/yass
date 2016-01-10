package ch.softappeal.yass.util;

@FunctionalInterface public interface Closer extends AutoCloseable {

    /**
     * This method is idempotent.
     */
    @Override void close();

}
