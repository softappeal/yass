package ch.softappeal.yass.tutorial.contract.generic;

import ch.softappeal.yass.Nullable;

public final class GenericEchoServiceImpl implements GenericEchoService {

    private GenericEchoServiceImpl() {
        // disable
    }

    @Override public @Nullable Pair<Boolean, TripleWrapper> echo(final @Nullable Pair<Boolean, TripleWrapper> value) {
        return value;
    }

    public static final GenericEchoService INSTANCE = new GenericEchoServiceImpl();

}
