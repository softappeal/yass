package ch.softappeal.yass.tutorial.contract.generic;

public class Pair<F, S> {

    public final F first;
    public final S second;

    public Pair(final F first, final S second) {
        this.first = first;
        this.second = second;
    }

}
