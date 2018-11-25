package ch.softappeal.yass.tutorial.contract.generic;

public final class GenericEchoServiceImpl implements GenericEchoService {

    private GenericEchoServiceImpl() {
        // disable
    }

    @Override
    public Pair<Boolean, TripleWrapper> echo(final Pair<Boolean, TripleWrapper> value) {
        return value;
    }

    public static final GenericEchoService INSTANCE = new GenericEchoServiceImpl();

}
