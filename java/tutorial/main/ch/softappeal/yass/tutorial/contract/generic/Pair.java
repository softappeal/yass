package ch.softappeal.yass.tutorial.contract.generic;

import ch.softappeal.yass.util.Nullable;

public class Pair<F, S> {

    public final @Nullable F first;
    public final @Nullable S second;

    public Pair(final @Nullable F first, final @Nullable S second) {
        this.first = first;
        this.second = second;
    }

}
