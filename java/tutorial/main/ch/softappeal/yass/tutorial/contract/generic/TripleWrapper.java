package ch.softappeal.yass.tutorial.contract.generic;

import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.tutorial.contract.PriceKind;

import java.util.List;

public final class TripleWrapper {

    public final @Nullable Triple<PriceKind, Pair<String, List<PairBoolBool>>> triple;

    public TripleWrapper(@Nullable final Triple<PriceKind, Pair<String, List<PairBoolBool>>> triple) {
        this.triple = triple;
    }

}
