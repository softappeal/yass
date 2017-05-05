package ch.softappeal.yass.tutorial.contract.generic;

import ch.softappeal.yass.util.Nullable;

public interface GenericEchoService {

    @Nullable Pair<Boolean, TripleWrapper> echo(@Nullable Pair<Boolean, TripleWrapper> value);

}
