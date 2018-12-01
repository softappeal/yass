package ch.softappeal.yass.tutorial.contract;

import java.util.*;

public abstract class Instrument {

    public final int id;
    public final String name;

    protected Instrument(final int id, final String name) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
    }

}
