package ch.softappeal.yass.tutorial.contract;

import java.util.Objects;

public final class SystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public final String details;

    public SystemException(final String details) {
        this.details = Objects.requireNonNull(details);
    }

    @Override public String getMessage() {
        return details;
    }

}
