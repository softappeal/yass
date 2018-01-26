package ch.softappeal.yass.tutorial.shared;

import ch.softappeal.yass.tutorial.contract.EchoService;
import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.TimeUnit;

public final class EchoServiceImpl implements EchoService {

    private EchoServiceImpl() {
        // disable
    }

    @Override public @Nullable Object echo(final @Nullable Object value) {
        if ("throwRuntimeException".equals(value)) {
            throw new RuntimeException("throwRuntimeException");
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    public static final EchoService INSTANCE = new EchoServiceImpl();

}
