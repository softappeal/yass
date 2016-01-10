package ch.softappeal.yass.util;

public interface Closer extends AutoCloseable {

    /**
     * This method is idempotent.
     */
    @Override void close();

}
