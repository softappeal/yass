package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.TimeUnit;

public final class EchoServiceImpl implements EchoService {

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

}
