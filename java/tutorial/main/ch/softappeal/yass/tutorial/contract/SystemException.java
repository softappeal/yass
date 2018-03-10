package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.fast.FastSerializer;

import java.util.Objects;

public final class SystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public final String message;

    public SystemException(final String message) {
        this.message = Objects.requireNonNull(message);
    }

    /**
     * @see FastSerializer
     */
    @Override public String getMessage() {
        return message;
    }

}
