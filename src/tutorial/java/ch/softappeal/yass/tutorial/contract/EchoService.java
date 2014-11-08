package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Nullable;

public interface EchoService {

    @Nullable Object echo(@Nullable Object value);

}
