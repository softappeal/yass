package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

@Tag(14) public final class SystemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Tag(1) public String message;

    public SystemException(final String message) {
        this.message = Check.notNull(message);
    }

    /**
     * @see AbstractFastSerializer
     */
    @Override public String getMessage() {
        return message;
    }

}
