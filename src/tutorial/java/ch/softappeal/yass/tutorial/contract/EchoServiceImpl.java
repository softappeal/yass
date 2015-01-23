package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Nullable;

import java.util.concurrent.TimeUnit;

public final class EchoServiceImpl implements EchoService {

    @Nullable @Override public Object echo(@Nullable final Object value) {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("echo: " + value);
        return value;
    }

}
