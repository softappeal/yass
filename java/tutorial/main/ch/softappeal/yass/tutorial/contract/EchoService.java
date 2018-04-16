package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.Nullable;

public interface EchoService {

    @Nullable Object echo(@Nullable Object value);

}
