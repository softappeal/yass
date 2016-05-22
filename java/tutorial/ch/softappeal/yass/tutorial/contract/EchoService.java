package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;

public interface EchoService {

    @Tag(0) @Nullable Object echo(@Nullable Object value);

}
