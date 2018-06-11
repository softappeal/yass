package ch.softappeal.yass.tutorial.contract.generic;

public final class Triple<F, T> extends Pair<F, Boolean> {

    public final T third;

    public Triple(final F first, final boolean second, final T third) {
        super(first, second);
        this.third = third;
    }

}
