package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.fast.FastSerializer;
import ch.softappeal.yass.util.Check;

public final class SystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public final String message;

    public SystemException(final String message) {
        this.message = Check.notNull(message);
    }

    /**
     * @see FastSerializer
     */
    @Override public String getMessage() {
        return message;
    }

}
