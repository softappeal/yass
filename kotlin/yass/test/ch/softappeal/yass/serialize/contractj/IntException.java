package ch.softappeal.yass.serialize.contractj;

import ch.softappeal.yass.Tag;

@Tag(42) public class IntException extends Exception {

    private static final long serialVersionUID = 1L;

    @Tag(1) public int value;

    public IntException(final int value) {
        this.value = value;
    }

    public IntException() {
        // empty
    }

}
