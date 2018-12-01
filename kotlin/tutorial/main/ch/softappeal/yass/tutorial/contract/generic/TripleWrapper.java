package ch.softappeal.yass.tutorial.contract.generic;

import ch.softappeal.yass.tutorial.contract.*;

import java.util.*;

public final class TripleWrapper {

    public final Triple<PriceKind, Pair<String, List<PairBoolBool>>> triple;

    public TripleWrapper(final Triple<PriceKind, Pair<String, List<PairBoolBool>>> triple) {
        this.triple = triple;
    }

}
