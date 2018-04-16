package ch.softappeal.yass;

@FunctionalInterface public interface Closer extends AutoCloseable {

    /**
     * This method is idempotent.
     */
    @Override void close();

}
