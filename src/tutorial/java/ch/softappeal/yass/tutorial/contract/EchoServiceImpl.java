package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Nullable;

public final class EchoServiceImpl implements EchoService {

    @Nullable @Override public Object echo(@Nullable final Object value) {
        return value;
    }

}
